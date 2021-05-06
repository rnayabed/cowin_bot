package in.rnayabed.cowin_bot.vaccine;

public class Vaccine
{
    private String centerName;
    private String centerAddress;
    private String amountLeft;
    private String vaccineName;
    private String ageLimit;


    public Vaccine(String centerName, String centerAddress,
                   String amountLeft, String vaccineName, String ageLimit)
    {
        this.centerName = centerName;
        this.centerAddress = centerAddress;
        this.amountLeft = amountLeft;
        this.vaccineName = vaccineName;
        this.ageLimit = ageLimit;
    }

    public String getCenterName() {
        return centerName;
    }

    public String getCenterAddress() {
        return centerAddress;
    }


    public String getAgeLimit() {
        return ageLimit;
    }

    public String getAmountLeft() {
        return amountLeft;
    }

    public String getVaccineName() {
        return vaccineName;
    }
}
