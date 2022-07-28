package stepdefinitions;
import io.restassured.response.Response;
import org.junit.Test;
import static io.restassured.RestAssured.*;

public class JdsApi {

    /*
    Your unique API key is: extgdktcesjpxwBdqyerkiArrvsypmn


For the 'api_jds_inventory' endpoint use the 'num' value of: cAgoEtedwjAtAbuGDmaFfFncvhcnmyv

For the 'api_jds_inv_by_loc' endpoint use the 'num' value of: BnzlGliFitArFrdpoxGcyocgqymlntv

HOUSTON LOCATION :  "LOCATION": "4",
     */
    Response response;

    @Test
    public void jdsApiConnect(){

       response= given().header("Content-Type","application/json").body("GFT657,GFT652").
        when().post("https://php.jdsindustries.com/public/api/api_get_data.php?api=api_jds_inventory&num=cAgoEtedwjAtAbuGDmaFfFncvhcnmyv&format=JSON&custNo=93599&custToken=extgdktcesjpxwBdqyerkiArrvsypmn");


       response.prettyPrint();
    }
}
