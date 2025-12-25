package flows;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.Gson;

import FlyModules.BrowserContants;
import FlyModules.Flynas;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import pageObjects.BaseClass;
import pageObjects.Database;

public class XYSRP_Flow {
	static WebDriver driver;
	boolean status;
	private Database PnrDetails;
	static String strDate;
	public static String FlynasURL;



	@Test(priority = 1)
	public void test() throws Exception {
	    setRestAssuredBaseURI();
	    RequestSpecification request = RestAssured.given();
	    request.header("Content-Type", "text/json");
	    Response response = request.get("/Getallroutesbyairline?airline=xy&days=28&skipdays=27&orderby=asc");
	    //System.out.println("Response body: " + response.body().asString());
	    String s = response.body().asString();
	    //System.out.println(s);
	    int statusCode = response.getStatusCode();
	    System.out.println("The status code received: " + statusCode);

	    Gson gson = new Gson();
	    Database[] databaseArray = gson.fromJson(s, Database[].class);
	    List<Database> databaseList = Arrays.asList(databaseArray);

	    // Ensure there are at least 40 routes to avoid IndexOutOfBoundsException
	    if (databaseList.size() >= 76) {
	        // Get routes from 31st to 40th (index 30 to 39)
	        List<Database> limitedDatabaseList = databaseList.subList(0, 40);

	        // Print the routes first
	        for (Database data : limitedDatabaseList) {
	            System.out.println(""+data.From+" - "+data.To);
	        }
	        
	        for (Database data : limitedDatabaseList) {
	            
				try {
					Date depDate = new SimpleDateFormat("dd MMM yyyy").parse(data.DepartureDate);
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
					String strDate = formatter.format(depDate);
					System.out.println("strDate :" + strDate);
					FlynasURL = "https://booking.flynas.com/#/booking/search-redirect?origin=" + data.From+ "&destination=" + data.To + "&currency=SAR&departureDate=" + strDate+ "&flightMode=oneway&adultCount=1&childCount=0&infantCount=0&promoCode=&culture=en-US&reference=WEGO&utm_source=wego&utm_medium=cpa&utm_campaign=core_meta";
					System.out.println("API URL " + FlynasURL);
					PnrDetails = data;
					FirefoxOptions options = new FirefoxOptions();
					options.addPreference("layout.css.devPixelsPerPx", "0.3");
					options.addPreference("permissions.default.image", 2);
					options.addArguments("--headless");
					driver = new FirefoxDriver(options);
					driver.manage().window().maximize();
					driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
					driver.manage().deleteAllCookies();
					driver.get(FlynasURL);
					new BaseClass(driver);
					Flynas.FlightDetails(driver, PnrDetails);
					driver.quit();

	            } catch (Exception e) {
	                
	            }
	        }
	    } else {
	        System.out.println("Not enough routes available.");
	    }
	}




	@AfterMethod
	public void teardown() {
		if (driver != null) {
			driver.quit();
		}
	}

	private void setRestAssuredBaseURI() {
		if (BrowserContants.ENV.equals("PRD")) {
			RestAssured.baseURI = BrowserContants.PRD_API_URL;
			System.out.println(BrowserContants.PRD_API_URL);
		} else if (BrowserContants.ENV.equals("STG")) {
			RestAssured.baseURI = BrowserContants.STG_API_URL;
			System.out.println(BrowserContants.STG_API_URL);
		}
	}
}
