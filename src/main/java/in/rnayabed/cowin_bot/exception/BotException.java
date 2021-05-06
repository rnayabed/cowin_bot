package in.rnayabed.cowin_bot.exception;

public class BotException extends Exception
{
    private final String title;

    public BotException(String title)
    {
        super(title);
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }
}
