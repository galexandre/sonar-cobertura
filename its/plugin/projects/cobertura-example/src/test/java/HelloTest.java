import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class HelloTest {
  @Test
  public void shouldSayHello() throws InterruptedException {
    assertEquals("hi", new Hello("hi").say());
    Thread.sleep(1000); // This guarantees that execution time of test will not be too close to zero
  }

  @Test
  public void shouldNotFail() {
    assertEquals(true, true);
  }
}
