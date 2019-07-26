package yuan.paycard.exception;

public class UnauthException extends RuntimeException{
    public UnauthException(){};
    public UnauthException(String message) {
        super(message);
    }
}
