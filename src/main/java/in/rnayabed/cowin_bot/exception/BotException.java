package in.rnayabed.cowin_bot.exception;

public class BotException extends Exception
{
    private final String title;

    public BotException(Exception e)
    {
        this(e.getMessage(), e);
    }

    public BotException(String title, Exception e)
    {
        e.printStackTrace();
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
}
