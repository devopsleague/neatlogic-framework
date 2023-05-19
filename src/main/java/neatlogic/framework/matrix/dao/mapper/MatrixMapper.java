package neatlogic.framework.matrix.dao.mapper;

import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.matrix.dto.MatrixCiVo;
import neatlogic.framework.matrix.dto.MatrixExternalVo;
import neatlogic.framework.matrix.dto.MatrixViewVo;
import neatlogic.framework.matrix.dto.MatrixVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MatrixMapper {

    MatrixVo getMatrixByUuid(String uuid);

    int checkMatrixIsExists(String uuid);

    int checkMatrixNameIsRepeat(MatrixVo matrixVo);

    int checkMatrixLabelIsRepeat(MatrixVo matrixVo);

    List<MatrixVo> getMatrixListByUuidList(List<String> uuidList);

    int searchMatrixCount(MatrixVo matrixVo);

    List<MatrixVo> searchMatrix(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("startNum") Integer startNum,
            @Param("pageSize") Integer pageSize
    );

    MatrixExternalVo getMatrixExternalByMatrixUuid(String matrixUuid);

    MatrixViewVo getMatrixViewByMatrixUuid(String matrixUuid);

    int getMatrixViewCount();

    List<MatrixViewVo> getMatrixViewList(BasePageVo searchVo);

    MatrixCiVo getMatrixCiByMatrixUuid(String matrixUuid);

    int insertMatrix(MatrixVo matrixVo);

    int replaceMatrixExternal(MatrixExternalVo matrixExternalVo);

    int insertMatrixView(MatrixViewVo matrixViewVo);

    int replaceMatrixCi(MatrixCiVo matrixCiVo);

    int updateMatrixNameAndLcu(MatrixVo matrixVo);

    int deleteMatrixByUuid(String uuid);

    int deleteMatrixExternalByMatrixUuid(String matrixUuid);

    int deleteMatrixViewByMatrixUuid(String matrixUuid);

    int deleteMatrixCiByMatrixUuid(String matrixUuid);
}
