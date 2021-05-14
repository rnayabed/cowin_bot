module in.rnayabed.cowin_bot
{
    requires java.logging;

    requires org.seleniumhq.selenium.java;
    requires org.seleniumhq.selenium.firefox_driver;
    requires org.seleniumhq.selenium.chrome_driver;
    requires org.seleniumhq.selenium.edge_driver;
    requires org.seleniumhq.selenium.api;
    requires org.seleniumhq.selenium.chromium_driver;

    requires org.seleniumhq.selenium.support;

    requires javax.mail.api;

    exports in.rnayabed.cowin_bot;
}
