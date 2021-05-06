package in.rnayabed.cowin_bot.logger;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class Formatter extends SimpleFormatter
{
    @Override
    public String format(LogRecord record)
    {
        return "["+Thread.currentThread().getName()+"] :: "+ record.getSourceClassName()+" @ "+record.getSourceMethodName()+" -> " + ":" + record.getLevel() + " = "+ record.getMessage() + "\r\n";
    }
}
