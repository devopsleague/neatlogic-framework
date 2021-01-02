package codedriver.framework.util;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.common.audit.AuditVoHandler;
import codedriver.framework.common.util.FileUtil;
import codedriver.framework.exception.file.FilePathIllegalException;
import codedriver.framework.file.core.LocalFileSystemHandler;
import codedriver.framework.file.core.MinioFileSystemHandler;
import codedriver.framework.restful.dao.mapper.ApiMapper;
import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AuditUtil {

    private static ApiMapper apiMapper;

    @Autowired
    public void setApiMapper(ApiMapper _apiMapper) {
        apiMapper = _apiMapper;
    }

    /*查看审计记录时可显示的最大字节数，超过此数需要下载文件后查看*/
    public final static long maxFileSize = 1024 * 1024;

    private static Logger logger = LoggerFactory.getLogger(AuditUtil.class);

    public static void saveAuditDetail(AuditVoHandler vo, String fileType) {
        /*
          组装文件内容JSON并且计算文件中每一块内容的开始坐标和偏移量
          例如参数的开始坐标为"param>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"的字节数
          偏移量为apiAuditVo.getParam()的字节数(注意一定要用UTF-8格式，否则计算出来的偏移量不对)
         */
        String paramFilePath = null;
        String resultFilePath = null;
        String errorFilePath = null;
        StringBuilder sb = new StringBuilder();
        sb.append("param>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        sb.append("\n");
        if (StringUtils.isNotBlank(vo.getParam()) && !vo.getParam().equals("{}") && !vo.getParam().equals("[]")) {
            int offset = vo.getParam().getBytes(StandardCharsets.UTF_8).length;
            paramFilePath = "?startIndex=" + sb.toString().getBytes(StandardCharsets.UTF_8).length + "&offset=" + offset;
            sb.append(vo.getParam());
            sb.append("\n");
        }
        sb.append("param<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        sb.append("\n");
        sb.append("result>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        sb.append("\n");
        String resultStr = null;
        //System.out.println(vo.getResult());
        if (vo.getResult() != null && StringUtils.isNotBlank(resultStr = JSON.toJSON(vo.getResult()).toString())) {
            int offset = resultStr.getBytes(StandardCharsets.UTF_8).length;
            resultFilePath = "?startIndex=" + sb.toString().getBytes(StandardCharsets.UTF_8).length + "&offset=" + offset;
            sb.append(resultStr);
            sb.append("\n");
        }
        sb.append("result<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        sb.append("\n");
        sb.append("error>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        sb.append("\n");
        if (StringUtils.isNotBlank(vo.getError())) {
            int offset = vo.getError().getBytes(StandardCharsets.UTF_8).length;
            errorFilePath = "?startIndex=" + sb.toString().getBytes(StandardCharsets.UTF_8).length + "&offset=" + offset;
            sb.append(vo.getError());
            sb.append("\n");
        }
        sb.append("error<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        String fileHash = DigestUtils.md5DigestAsHex(sb.toString().getBytes());
        String filePath = null;
        filePath = apiMapper.getAuditFileByHash(fileHash);
        /* 如果在audit_file表中找到文件路径，说明此次请求与之前某次请求完全一致，则不再重复生成日志文件 */
        if (StringUtils.isBlank(filePath)) {
            InputStream inputStream = IOUtils.toInputStream(sb.toString(), StandardCharsets.UTF_8);
            try {
                filePath = FileUtil.saveData(MinioFileSystemHandler.NAME, TenantContext.get().getTenantUuid(), inputStream, fileHash, "text/plain", fileType);
            } catch (Exception e) {
                logger.error("Minio访问失败，自动切换成本地存储模式");
                try {
                    filePath = FileUtil.saveData(LocalFileSystemHandler.NAME, TenantContext.get().getTenantUuid(), inputStream, fileHash, "text/plain", fileType);
                } catch (Exception e1) {
                    logger.error(e1.getMessage(), e1);
                }
            } finally {
                if (StringUtils.isNotBlank(filePath)) {
                    apiMapper.insertAuditFile(fileHash, filePath);
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (StringUtils.isNotBlank(filePath)) {
            vo.setParamFilePath(StringUtils.isNotBlank(paramFilePath) ? (filePath + paramFilePath) : null);
            vo.setResultFilePath(StringUtils.isNotBlank(resultFilePath) ? (filePath + resultFilePath) : null);
            vo.setErrorFilePath(StringUtils.isNotBlank(errorFilePath) ? (filePath + errorFilePath) : null);
        }
    }

    public static String getAuditDetail(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            throw new FilePathIllegalException("文件路径不能为空");
        }
        if (!filePath.contains("?") || !filePath.contains("&") || !filePath.contains("=")) {
            throw new FilePathIllegalException("文件路径格式错误");
        }
        String result = null;
        String path = filePath.split("\\?")[0];
        String[] indexs = filePath.split("\\?")[1].split("&");
        long startIndex = Long.parseLong(indexs[0].split("=")[1]);
        long offset = Long.parseLong(indexs[1].split("=")[1]);

        try (InputStream in = FileUtil.getData(path)) {
            if (in != null) {
                /*
                 * 如果偏移量大于最大字节数限制，那么就只截取最大字节数长度的数据
                 */
                int buffSize = 0;
                if (offset > maxFileSize) {
                    buffSize = (int) maxFileSize;
                } else {
                    buffSize = (int) offset;
                }

                in.skip(startIndex);
                byte[] buff = new byte[buffSize];
                in.read(buff);

                result = new String(buff, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return result;
    }

    public static void downLoadAuditDetail(HttpServletRequest request, HttpServletResponse response, String filePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new FilePathIllegalException("文件路径不能为空");
        }
        if (!filePath.contains("?") || !filePath.contains("&") || !filePath.contains("=")) {
            throw new FilePathIllegalException("文件路径格式错误");
        }
        String path = filePath.split("\\?")[0];
        String[] indexs = filePath.split("\\?")[1].split("&");
        long startIndex = Long.parseLong(indexs[0].split("=")[1]);
        long offset = Long.parseLong(indexs[1].split("=")[1]);

        try (InputStream in = FileUtil.getData(path)) {
            if (in != null) {
                in.skip(startIndex);

                String fileNameEncode = "AUDIT_DETAIL.log";
                boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
                if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
                    fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
                } else {
                    fileNameEncode = new String(fileNameEncode.getBytes(StandardCharsets.UTF_8), "ISO8859-1");
                }
                response.setContentType("aplication/x-msdownload;charset=utf-8");
                response.setHeader("Content-Disposition", "attachment;fileName=\"" + fileNameEncode + "\"");
                OutputStream os = response.getOutputStream();

                byte[] buff = new byte[(int) maxFileSize];
                int len = 0;
                long endPoint = 0;
                while ((len = in.read(buff)) != -1) {
                    /*
                     * endPoint用来记录累计读取到的字节数
                     * 如果大于偏移量，说明实际读到的数据超过了需要的数据
                     * 那么就需要减掉多读出来的数据
                     */
                    endPoint += len;
                    if (endPoint > offset) {
                        len = (int) (len - (endPoint - offset));
                    }
                    os.write(buff, 0, len);
                    os.flush();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
