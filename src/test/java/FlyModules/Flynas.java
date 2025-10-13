package FlyModules;
 
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import flows.XYSRP_Flow;
import pageObjects.Database;


public class Flynas extends XYSRP_Flow {
	
	static String NoFlights;
	static String result;
	static String F3Dummy;
	static String From;
	static String To;
	static String Flights;
	static String Depdate=null;
	
	public static void search(WebDriver driver) throws Exception
	{

		driver.get("https://accounts.google.com/v3/signin/identifier?dsh=S873427101%3A1670174877878096&continue=https%3A%2F%2Fmail.google.com%2Fmail%2F&flowEntry=ServiceLogin&flowName=GlifWebSignIn&rip=1&sacu=1&service=mail&ifkv=ARgdvAv7qIg9j-X7zxwLWrETGRTaquhiB_tbb7YW19ONpQZ-z4IHi9LknQfITZIbwMLY0zXURVL5jg");
	    Thread.sleep(3000);
		
		
	}
	
	public static WebDriver FlightDetails(WebDriver driver, Database PnrDetails) throws Exception {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
	    boolean isPageLoaded = false;
	    int maxAttempts = 2;
	    int attempt = 1;

	    while (!isPageLoaded && attempt <= maxAttempts) {
	        try {
	            // Wait for the Flynas booking page to load
	            isPageLoaded = wait.until(
	                    ExpectedConditions.urlContains("https://booking.flynas.com/#/booking/flights")
	            );
	        } catch (Exception e) {
	            System.out.println("Page didn't load within 25 seconds on attempt " + attempt);

	            // Fetch current URL
	            String currentUrl = "";
	            try {
	                currentUrl = driver.getCurrentUrl();
	                System.out.println("Current URL before retry: " + currentUrl);
	            } catch (Exception urlEx) {
	                System.out.println("Unable to fetch current URL.");
	            }

	            // If 500 error page, quit and skip
	            if ("https://www.flynas.com/en/error-500".equalsIgnoreCase(currentUrl)) {
	                System.out.println("❌ Error 500 page detected. Closing browser and skipping this route...");
	                try {
	                    driver.quit();
	                } catch (Exception quitEx) {
	                    System.out.println("Error while quitting browser: " + quitEx.getMessage());
	                }
	                return null; // skip route
	            } else {
	                // Normal retry: delete cookies, refresh page
	                Flynas.search(driver);
	                Flynas.search(driver);
	                driver.get(FlynasURL);
	                Thread.sleep(6000);
	                System.out.println("Cookies deleted. Page refreshed.");
	            }
	        }
	        attempt++;
	    }

	    if (!isPageLoaded) {
	        System.out.println("❌ Page didn't load after " + maxAttempts + " attempts.");
	        return driver;
	    } else {
	        System.out.println("✅ Page loaded successfully.");
	    }

	    Actions actions = new Actions(driver);

	    // Wait for 'Back' button to appear
	    boolean displayed = false;
	    do {
	        try {
	            displayed = driver.findElement(By.xpath("//button[contains(text(),'Back')]")).isDisplayed();
	        } catch (Exception e) {
	            driver.get(FlynasURL);
	            Thread.sleep(4000);
	        }
	    } while (!displayed);

	    String departureAirport = PnrDetails.From;
	    String arrivalAirport = PnrDetails.To;

	    String[] airportsToHandle = {"CAI"};

	    if (ArrayUtils.contains(airportsToHandle, departureAirport) || ArrayUtils.contains(airportsToHandle, arrivalAirport)) {
	        try {
	            Thread.sleep(2000);
	            WebElement NoThanks = driver.findElement(By.xpath("//*/text()[normalize-space(.)='No Thanks!']/parent::*"));
	            NoThanks.click();
	            Thread.sleep(2000);
	        } catch (Exception e) {
	            // ignore
	        }
	    }

	    try {
	        WebElement elementToMoveAndClick = driver.findElement(By.xpath("//div[@class='pers-modal__btn-close-icon']"));
	        actions.moveToElement(elementToMoveAndClick).perform();
	        elementToMoveAndClick.click();
	    } catch (Exception e) {
	        // ignore
	    }

	    // Get From & To city text
	    WebElement FromCity = driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Modify your Search'])[1]/preceding::span[7]"));
	    From = FromCity.getText();
	    WebElement ToCity = driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Modify your Search'])[1]/preceding::span[5]"));
	    To = ToCity.getText();

	    // Replace special airport codes
	    if ("TR1".equals(From)) { From = "SAW"; PnrDetails.From = "SAW"; }
	    if ("TR1".equals(To)) { To = "SAW"; PnrDetails.To = "SAW"; }
	    if ("EG1".equals(From)) { From = "CAI"; PnrDetails.From = "CAI"; }
	    if ("EG1".equals(To)) { To = "CAI"; PnrDetails.To = "CAI"; }
	    if ("AE1".equals(From)) { From = "DXB"; PnrDetails.From = "DXB"; }
	    if ("AE1".equals(To)) { To = "DXB"; PnrDetails.To = "DXB"; }

	    try {
	        driver.findElement(By.xpath("//a[contains(@class, 'btn_prev')]")).click();
	        int dayCounter = 1;

	        for (int weekOffset = 0; weekOffset < 10; weekOffset++) {
	            for (int dayOffset = 1; dayOffset <= 5; dayOffset++) {
	                int totalOffset = weekOffset * 5 + dayOffset;
	                if (totalOffset > 11) break;

	                driver.findElement(By.xpath("//a[@class='btn-refresh']")).click();

	                String DepartDate = driver.findElement(By.xpath("//*[@id='select_departure']/div/ul/li[" + dayCounter + "]/a/span[2]")).getText();
	                String[] dateParts = DepartDate.split("\\W+");
	                int dayInt = Integer.parseInt(dateParts[1]);
	                String day = String.format("%02d", dayInt);
	                String monthAbbreviation = dateParts[2];
	                String Year = "2025";
	                Depdate = String.format("%s %s %s", day, monthAbbreviation, Year);

	                System.out.println("SRP Date: " + Depdate);

	                String FlightsAvailable = driver.findElement(By.xpath("//*[@id='select_departure']/div/ul/li[" + dayCounter + "]/a/span[1]/span/span")).getText().replaceAll("[\r\n]+", " ");
	                boolean isFlightsAvailable = !FlightsAvailable.contains("Sold") && !FlightsAvailable.contains("No");

	                if (isFlightsAvailable) {
	                    driver.findElement(By.xpath("//*[@id='select_departure']/div/ul/li[" + dayCounter + "]/a/span[1]/span/span")).click();
	                    Thread.sleep(2000);
	                    SRP_Flights(driver, PnrDetails);
	                } else {
	                    System.out.println("No Flights");
	                    List<FadFlightDetails> finalList = new ArrayList<>();
	                    ApiMethods.sendResults(PnrDetails.From, PnrDetails.To, Depdate, finalList);
	                }

	                dayCounter++;
	                if (dayOffset == 5 && weekOffset < 9) {
	                    driver.findElement(By.xpath("//a[@class='btn-refresh']")).click();
	                    driver.findElement(By.xpath("//a[contains(@class, 'btn_next')]")).click();
	                    Thread.sleep(1000);
	                }
	            }
	        }

	    } catch (Exception e) {
	        driver.manage().deleteAllCookies();
	        driver.get("https://www.iana.org/domains");
	    }

	    return driver; // ✅ always return driver at the end
	}
	
	public static void SRP_Flights(WebDriver driver,Database PnrDetails) throws Exception
	{
		
		/*WebElement FlightText=driver.findElement(By.xpath("//div[2]/div/div[2]/div"));
		Flights =FlightText.getText().replace(")Direct", ") Direct");		
		System.out.println(Flights);
		NoFlights= Flights.split(" ")[5];*/
		
		
		if(From.equals(PnrDetails.From))
		 { 
			System.out.println("From City Matched");
			
			if(To.equals(PnrDetails.To))
			 {
				System.out.println("To City Matched");
				
				if("EG1".equals(PnrDetails.From))
				 {
					PnrDetails.From="CAI";
				 }
				else if("EG1".equals(PnrDetails.To))
				 {
					PnrDetails.To="CAI";
				 }
			     else{
			    	
			     }
				if("AE1".equals(PnrDetails.From))
				 {
					PnrDetails.From="DXB";
				 }
				else if("AE1".equals(PnrDetails.To))
				 {
					PnrDetails.To="DXB";
				 }
			     else{
			    	
			     }
				
				XY_FlightDetailsSending_Economy(driver, PnrDetails);
				
				/*if("a".equals(NoFlights))
				 { 
					//driver.manage().deleteAllCookies();
					System.out.println("No Flights Available");
					No_Flights(driver, PnrDetails);
					NoFlights ="null";
					Flynas.search(driver);
				 }
				else if("(0)".equals(NoFlights))
				{
					//driver.manage().deleteAllCookies();
					System.out.println("No Direct Flights");
					No_Flights(driver, PnrDetails);
					NoFlights =" ";
				}
				
				
				else {
					//System.out.println("ECONOMY AND PREMIUM FLIGHT");
					XY_FlightDetailsSending_Economy(driver, PnrDetails);
					//XY_FlightDetailsSending_Economy_Premium(driver, PnrDetails);
				}*/
			 }
			else {
				System.out.println("To City Mismatch");
				Thread.sleep(3000);
				driver.get(FlynasURL); 
				Thread.sleep(1000);
				FlightDetails(driver, PnrDetails);
				
		        }
		       }
			else {
				
				System.out.println("From City Mismatch");
				Thread.sleep(3000);
				driver.get(FlynasURL); 
				Thread.sleep(1000);
				FlightDetails(driver, PnrDetails);
				
		 }
	}
		
public static void XY_FlightDetailsSending_Economy(WebDriver driver,Database PnrDetails) throws Exception
{
	//flyadealPage.flight_Details();
	String DataChanege=null;
	String date = null;
	String month = null;
	String year = null;
	String FlightNum = null;
	String JournyTimeHours = null;
	 String JournyTimeMin=null;
	 String EndTime=null;
	 String From=PnrDetails.From;
	 String To=PnrDetails.To;
	 String flySeatNum="99";
	 String economy=null;
	 String premium=null;
	 String StartTerminal=null;
	 String EndTerminal=null;

	 String flyPlusSeatNum="99";
	 String Sold=null;
	 List<FadFlightDetails> finalList =  new ArrayList<FadFlightDetails>();
	 //driver.findElement(By.xpath("//*[@id='collapseOne0']/div/div[2]/div[1]/button[2]")).click();
	 //Thread.sleep(1500);
	 //WebDriverWait wait = new WebDriverWait(driver, 10);
     //WebElement directFlightsButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Direct Flights')]")));

     // Click on the button
     //directFlightsButton.click();
		
	try {
		String ele = null;
		List<WebElement> element = driver.findElements(By.xpath("//div[@class='flight_table']/div[@class='card px-0']"));
		 for (WebElement e1 : element) {
				 ele = e1.getText();
				 //System.out.println(ele);
				 FadFlightDetails currentFlightFly = new FadFlightDetails();
				 FadFlightDetails currentFlightFlyPlus = new FadFlightDetails();
				 
				 
				 String str1=ele.replaceAll("[\r\n]+", " ").replace(",", "");
				 String s=str1.replaceAll("Non-stop ","").replaceAll("Flight Details ", "").replaceAll("XY ", "").replaceAll("SAR", "").replaceAll("hr ", "").replaceAll("min ", "")
						 .replaceAll("from ", "").replaceAll("Sold out", "Soldout").replaceAll("Boeing 737-LR1 ", "").replaceAll("Boeing 737-LR2 ", "").replaceAll("Dubai-Al Maktoum", "Dubai-AlMaktoum").replaceAll("New Delhi", "NewDelhi").replaceAll("XY", "").replaceAll("Doha Hamad International Airport", "DohaHamadInternationalAirport").replaceAll("Istanbul Sabiha Gokcen Airport", "IstanbulSabiha").replaceAll("Airbus 320 | Operated by flynas ", "").replaceAll("Boeing 739-739 ", "").replaceAll("Cairo International Airport ", "").replaceAll("Airbus 320 ", "").replaceAll("Airbus 330 ", "").replaceAll("Sharm el Sheikh", "SharmelSheikh").replaceAll("Al Jouf", "AlJouf").replaceAll("Cairo Sphinx Airport", "CairoSphinxAirport").replaceAll("Doha Hamad international Airport", "DohaHamadinternationalAirport").replaceAll("Istanbul Sabiha Gokcen International Airport", "IstanbulSabihaGokcenInternationalAirport").replaceAll("King Abdulaziz International Airport", "KingAbdulazizInternationalAirport").replaceAll("King Khalid International Airport", "KingKhalidInternationalAirport").replaceAll("Hamad International Airport", "HamadInternationalAirport").replaceAll("Sharm El-Sheikh", "SharmElSheikh").replaceAll("Istanbul Sabiha", "IstanbulSabiha").replaceAll("Al Baha", "AlBaha").replaceAll("Abu Dhabi", "AbuDhabi").replaceAll("332 Jeddah", "Jeddah").replaceAll("Promo ", "").replaceAll("Airbus 3\\d{2} ", "");
				 
				 
				 String Str = new String(s);
			      
				 String[] flightDetails = s.split("\n");
		            
		        
			   //894 Dammam 19:50 3 25 Cairo 22:15 369.00 899.00	  
				//57 Riyadh 22:50 2 0 Jeddah 00:50 (+1) 219.01 414.00 Only 4 seats left
				// 57 Riyadh 22:50 2 0 Jeddah 00:50 (+1) Soldout 1554.00 Only 1 seats left
				// 3 Riyadh 05:25 2 0 Jeddah 07:25 409.00 Only 2 seats left 788.00 Only 2 seats left
				//61 Riyadh RUH 00:35 2 0 Jeddah JED 02:35 609.01
				//55 Riyadh RUH 23:45 2 0 Jeddah JED 01:45 (+1) 689.00
				 System.out.println(s);
				 String StartTime= s.split(" ")[3];
				 FlightNum= s.split(" ")[0];
				 String FromCity=s.split(" ")[2];
				 String ToCity=s.split(" ")[7];
				 
				 
				 if (FlightNum.length() == 1) {
			            result = "11" + FlightNum;
			            int resultInt = Integer.parseInt(result) * 3;
			            F3Dummy = "4" + Integer.toString(resultInt);
			            //System.out.println("Dummy Flight:"+F3Dummy);
			            
			        } else if (FlightNum.length() == 2) {
			            result = "1" + FlightNum;
			            int resultInt = Integer.parseInt(result) * 3;
			            F3Dummy = "4" + Integer.toString(resultInt);
			            //System.out.println("Dummy Flight:"+F3Dummy);
			        }
			        else{
			        	result = "1" + FlightNum;
			        	int resultInt = Integer.parseInt(result) * 3;
			        	String resultString = Integer.toString(resultInt);
			        	String lastThreeChars = resultString.substring(resultString.length() - 3);
			        	F3Dummy = "4" + lastThreeChars;
			        	//System.out.println("Dummy Flight: " + F3Dummy);
				         
			        }
				 
				 //61 Riyadh RUH 00:35 2 0 Jeddah JED 02:35 299.00
				 //System.out.println("FLIGHT NOOO:"+FlightNum);
				 EndTime= s.split(" ")[8];
				 JournyTimeHours=s.split(" ")[4];
				 JournyTimeMin=s.split(" ")[5];
				String PlusOne= s.split(" ")[9];
				String Seats = "9";	
				
				String[] splitArr = s.split(" ");
				if (splitArr.length >= 12) {
				    //Seats = splitArr[9];
					for (int i = 0; i < splitArr.length; i++) {
					    if ("Only".equals(splitArr[i]) && i + 1 < splitArr.length) {
					        Seats = splitArr[i + 1];
					        break; // Exit the loop once seats count is found
					    }
					}
				}

				//System.out.println("Seats count: " + Seats);
				
				if(PlusOne.equals("(+1)"))
				{
					
				 DataChanege="1";
				 economy=s.split(" ")[10];
				 if (splitArr.length >= 12) {
					    //Seats = splitArr[10];
					  for (int i = 0; i < splitArr.length; i++) {
					     if ("Only".equals(splitArr[i]) && i + 1 < splitArr.length) {
					         Seats = splitArr[i + 1];
					         break; // Exit the loop once seats count is found
					     }
					 }
					}

					//System.out.println("Seats count: " + Seats);
				
				// 57 Riyadh 22:50 2 0 Jeddah 00:50 (+1) Soldout 1554.00 Only 1 seats left
				 if("Soldout".equals(economy))
				 {
					  economy="00.0";
				 }
				 else{
					 
				 }
				//57 Riyadh 22:50 2 0 Jeddah 00:50 (+1) 529.00 Only 5 seats left 788.00 Only 1 seats left
					
				
				 
				
				}
				else{
					
					 economy=s.split(" ")[9];
					
					// 57 Riyadh 22:50 2 0 Jeddah 00:50 Soldout 1554.00 Only 1 seats left
					 if("Soldout".equals(economy))
					 {
						  economy="00.0";
					 }
					 else{
						 
					 }
					// 57 Riyadh 22:50 2 0 Jeddah 00:50 1554.00 Soldout Only 1 seats left
					
					 
					 
				}
				
				if ("00.0".equals(economy)) {
				    economy = "00.0";
				} else {
					
				}
				
				
			    
				     
				/*System.out.println("From:"+From);
				System.out.println("To:"+To);
				System.out.println("economy:"+economy);
				System.out.println("DepartureDate:"+Depdate);
				System.out.println("Currency:SAR");
				
				
				System.out.println("FlightNumber:"+FlightNum);
				System.out.println("Class :Economy");*/
				int Hours = Integer.parseInt(JournyTimeHours);	
				int TotalMin=Hours * 60;
				
				int Min = Integer.parseInt(JournyTimeMin);	
				int Total=TotalMin+Min;
				String JournyTimeTotal=Integer.toString(Total);
				/*System.out.println("JourneyTime:"+Total);
				
				
				
				System.out.println("StartTime :"+StartTime);
				System.out.println("EndTime :"+EndTime);
				System.out.println("StartDate:"+Depdate);
				System.out.println("EndDate:Null");
				System.out.println("Start Airport:"+From);
				System.out.println("End Airport:"+To);
				System.out.println("Fly Fare:"+economy);*/
				
				//System.out.println("----------------------------------------");
				
				currentFlightFlyPlus.FareType=currentFlightFly.FareType="Economy";
				currentFlightFlyPlus.Class=currentFlightFly.Class="Economy";
				currentFlightFlyPlus.StartAirp = currentFlightFly.StartAirp =FromCity;
				currentFlightFlyPlus.EndAirp=currentFlightFly.EndAirp=ToCity;
				currentFlightFlyPlus.StartDt=currentFlightFly.StartDt=Depdate;
				currentFlightFlyPlus.ADTBG=currentFlightFly.ADTBG="";
				currentFlightFlyPlus.CHDBG=currentFlightFly.CHDBG="";
				currentFlightFlyPlus.INFBG=currentFlightFly.INFBG="";
				currentFlightFlyPlus.DayChg=currentFlightFly.DayChg = DataChanege;
				currentFlightFlyPlus.Fltnum=currentFlightFly.Fltnum="XY"+FlightNum;
								
				/*if (element.size() >= 1) {
				    if (From.equals("TIF")) { 
				        if (To.equals("RUH")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        }
				    } else if (From.equals("RUH")) {
				        if (To.equals("TIF")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        } else if (To.equals("JED")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        }
				    } else if (From.equals("JED")) {
				        if (To.equals("RUH")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        } else if (To.equals("AHB")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        } else if (To.equals("DMM")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        } else if (To.equals("ELQ")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        }
				    } else if (From.equals("AHB")) {
				        if (To.equals("JED")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        }
				    } else if (From.equals("DMM")) {
				        if (To.equals("JED")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        }
				    } else if (From.equals("ELQ")) {
				        if (To.equals("JED")) {
				            currentFlightFlyPlus.Fltnum = currentFlightFly.AltFltnum = "F3" + F3Dummy;
				        }
				    }
				}

				else{
					
				}*/
				
				currentFlightFlyPlus.JrnyTm=currentFlightFly.JrnyTm=JournyTimeTotal;
				currentFlightFlyPlus.StartTm=currentFlightFly.StartTm=StartTime;
				currentFlightFlyPlus.EndTm=currentFlightFly.EndTm=EndTime;
				currentFlightFlyPlus.NoOfSeats=currentFlightFly.NoOfSeats=Seats;
				//currentFlightFlyPlus.StartTerminal=currentFlightFly.StartTerminal="";
				currentFlightFlyPlus.EndTerminal=currentFlightFly.EndTerminal="";
				currentFlightFlyPlus.AdultBasePrice=currentFlightFly.AdultBasePrice=economy.replace(",", "");
				currentFlightFlyPlus.AdultTaxes=currentFlightFly.AdultTaxes ="";
				currentFlightFlyPlus.ChildBasePrice=currentFlightFly.ChildBasePrice=economy.replace(",", "");
				currentFlightFlyPlus.ChildTaxes=currentFlightFly.ChildTaxes="";
				//currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="60";
				
				if (From.equals("AMM")) {
				    if (To.equals("RUH")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="413";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("CAI")) {
				    if (To.equals("DMM") || To.equals("JED") || To.equals("RUH")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="320";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("DEL")) {
				    if (To.equals("RUH")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="161";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("DMM")) {
				    if (To.equals("CAI") || To.equals("DXB")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="140";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("DOH")) {
				    if (To.equals("JED") || To.equals("RUH")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="130";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("DXB")) {
				    if (To.equals("DMM") || To.equals("JED") || To.equals("RUH")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="140";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("JED")) {
				    if (To.equals("CAI") || To.equals("DOH") || To.equals("AUH") || To.equals("DXB")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="140";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("KWI")) {
				    if (To.equals("RUH")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="151";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("LKO")) {
				    if (To.equals("RUH")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="161";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				    }
				} else if (From.equals("RUH")) {
				    if (To.equals("AMM") || To.equals("CAI") || To.equals("DEL") || To.equals("DOH") || To.equals("DXB") || To.equals("KWI") || To.equals("LKO") || To.equals("TBS")) {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="140";
				    } else {
				        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
				   }  
				  } 
				else {
			        currentFlightFlyPlus.InfantBasePrice=currentFlightFly.InfantBasePrice ="75";
			   }  
				
				currentFlightFlyPlus.InfantTaxes=currentFlightFly.InfantTaxes="";
				currentFlightFlyPlus.TotalApiFare=currentFlightFly.TotalApiFare="";
				finalList.add(currentFlightFly);
		        
				WebElement FlightText=driver.findElement(By.xpath("//div[2]/div/div[2]/div"));
				ele =FlightText.getText();
				
		 }
		 
	}
	
		
		 catch (Exception e) {
		
	}
		
	    System.out.println("----------------------------------------");
	    //driver.manage().deleteAllCookies();
		ApiMethods.sendResults(From, To,Depdate, finalList);
	
}

}









