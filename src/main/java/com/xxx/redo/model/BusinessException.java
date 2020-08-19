package com.xxx.redo.model;

/**
 * UredoException
 * 
 */
public class BusinessException extends RuntimeException {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5176582834954755702L;

    /**
     * 构造方法
     */
    public BusinessException() {
        super();
    }

    /**
     * 构造方法
     *
     * @param message 异常信息
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 构造方法
     *
     * @param message 异常信息
     * @param cause 异常类
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造方法
     *
     * @param cause 异常类
     */
    public BusinessException(Throwable cause) {
        super(cause);
    }
}
