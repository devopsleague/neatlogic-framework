package neatlogic.framework.exception.file;

import neatlogic.framework.exception.core.ApiRuntimeException;

public class ExcelMissColumnException extends ApiRuntimeException {

    private static final long serialVersionUID = 8336484981861050549L;

    public ExcelMissColumnException(String msg) {
        super("Excel中缺少{0}", msg);
    }
}
