package model;

public class Result {
    private boolean success;
    private String message;
    private Object object ;
    public Result(boolean success, String message,Object object) {
        this.success = success;
        this.message = message;
        this.object=object;
    }

    public boolean success() {
        return success;
    }
    public Object getObject(){return object;}
    public String message() {
        return message;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
