package ca.uhn.fhir.federator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ServerResourceConfig;
import ca.uhn.fhir.rest.annotation.Read;

public class TestResourceConfigEvaluator {
  @Test
  public void TestTrue() {

    ServerResourceConfig config = new ServerResourceConfig();
    config.setRead("true");
    config.setServer("http://test/fhir");

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, null, config, null, null)
            .execute();

    assertEquals(true, actual);
  }

  @Test
  public void TestFalse() {

    ServerResourceConfig config = new ServerResourceConfig();
    config.setRead("false");
    config.setServer("http://test/fhir");

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, null, config, null, null)
            .execute();

    assertEquals(false, actual);
  }

  @Test
  public void TestNull() {

    ServerResourceConfig config = new ServerResourceConfig();
    config.setRead(null);
    config.setServer("http://test/fhir");

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, null, config, null, null)
            .execute();

    assertEquals(true, actual);
  }

  @Test
  public void TestFhirPathWrong() {

    ServerResourceConfig config = new ServerResourceConfig();
    config.setRead("Patient.name=\"Decuypere\"");
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Collections.singletonList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(
                FhirContext.forR4Cached(), Read.class, patient, config, null, null)
            .execute();

    assertEquals(false, actual);
  }

  @Test
  public void TestFhirPathCorrect() {

    ServerResourceConfig config = new ServerResourceConfig();
    config.setRead("Patient.name.family=\"Decuypere\"");
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Collections.singletonList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(
                FhirContext.forR4Cached(), Read.class, patient, config, null, null)
            .execute();

    assertEquals(true, actual);
  }

  @Test
  public void TestFhirPathTypeMistake() {

    ServerResourceConfig config = new ServerResourceConfig();
    config.setRead("Patient.name.family");
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Collections.singletonList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(
                FhirContext.forR4Cached(), Read.class, patient, config, null, null)
            .execute();

    assertEquals(false, actual);
  }

  @Test
  public void TestFhirPathBogus() {

    ServerResourceConfig config = new ServerResourceConfig();
    config.setRead("blablabla");
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Collections.singletonList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(
                FhirContext.forR4Cached(), Read.class, patient, config, null, null)
            .execute();

    assertEquals(false, actual);
  }
}
