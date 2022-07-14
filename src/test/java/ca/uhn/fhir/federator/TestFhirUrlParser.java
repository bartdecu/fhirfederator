package ca.uhn.fhir.federator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.federator.FhirUrlParser.SContext;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.junit.jupiter.api.Test;

public class TestFhirUrlParser {

  class TestErrorListener implements ANTLRErrorListener {

    private boolean fail = false;

    public boolean isFail() {
      return fail;
    }

    public void setFail(boolean fail) {
      this.fail = fail;
    }

    @Override
    public void syntaxError(
        Recognizer<?, ?> arg0,
        Object arg1,
        int arg2,
        int arg3,
        String arg4,
        RecognitionException arg5) {
      setFail(true);
    }

    @Override
    public void reportContextSensitivity(
        Parser arg0, DFA arg1, int arg2, int arg3, int arg4, ATNConfigSet arg5) {
      setFail(true);
    }

    @Override
    public void reportAttemptingFullContext(
        Parser arg0, DFA arg1, int arg2, int arg3, BitSet arg4, ATNConfigSet arg5) {
      setFail(true);
    }

    @Override
    public void reportAmbiguity(
        Parser arg0, DFA arg1, int arg2, int arg3, boolean arg4, BitSet arg5, ATNConfigSet arg6) {
      setFail(true);
    }
  }

  private static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(TestFhirUrlParser.class);

  @Test
  public void testExploratoryString() throws IOException {

    String simplestProgram =
        "Patient?_has:Observation:patient:_has:AuditEvent:entity:agent:Practitioner.name=janedoe";

    List<List<String>> actual = toUrls(simplestProgram);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Patient?identifier={Observation.patient.identifier}",
                "Observation?identifier={AuditEvent.entity.identifier}",
                "AuditEvent?agent.identifier={Practitioner.identifier}",
                "Practitioner?name=janedoe"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString1() throws IOException {

    String simplestProgram = "DiagnosticReport?subject:Patient.name=Sarah";

    List<List<String>> actual = toUrls(simplestProgram);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "DiagnosticReport?subject.identifier={Patient.identifier}", "Patient?name=Sarah"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString2() throws IOException {

    String simplestProgram = "Encounter?subject=Patient/78a14cbe-8968-49fd-a231-d43e6619399f";

    List<List<String>> actual = toUrls(simplestProgram);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Encounter?subject.identifier={Patient.identifier}",
                "Patient?_id=78a14cbe-8968-49fd-a231-d43e6619399f"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString3() throws IOException {

    String simplestProgram = "Encounter?subject:Patient.birthdate=1987-02-20";

    List<List<String>> actual = toUrls(simplestProgram);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Encounter?subject.identifier={Patient.identifier}",
                "Patient?birthdate=1987-02-20"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString4() throws IOException {

    String simplestProgram = "Patient?birthdate:missing=true";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected = Arrays.asList(Arrays.asList("Patient?birthdate:missing=true"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString5() throws IOException {

    String simplestProgram =
        "Patient?general-practitioner:Practitioner.name=Sarah&general-practitioner:Practitioner.address-state=WA";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Patient?general-practitioner.identifier={Practitioner.identifier}",
                "Practitioner?name=Sarah"),
            Arrays.asList(
                "Patient?general-practitioner.identifier={Practitioner.identifier}",
                "Practitioner?address-state=WA"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString6() throws IOException {

    String simplestProgram =
        "Patient?identifier=https://github.com/synthetichealth/synthea|621338a9-01f4-49d4-b852-14507a8bf8c7";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Patient?identifier=https://github.com/synthetichealth/synthea|621338a9-01f4-49d4-b852-14507a8bf8c7"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString7() throws IOException {

    String simplestProgram = "Patient?name=Sarah&name=Jones";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(Arrays.asList("Patient?name=Sarah"), Arrays.asList("Patient?name=Jones"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString8() throws IOException {

    String simplestProgram = "Observation?subject.identifier=urn:oid:1.2.36.146.595.217.0.1|12345";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Observation?subject.identifier=urn:oid:1.2.36.146.595.217.0.1|12345"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString9() throws IOException {

    String simplestProgram = "Encounter?_id=1";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected = Arrays.asList(Arrays.asList("Encounter?_id=1"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString10() throws IOException {

    String simplestProgram =
        "Observation?subject:Patient.name=Hodges&code=http://loinc.org|29463-7";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Observation?subject.identifier={Patient.identifier}", "Patient?name=Hodges"),
            Arrays.asList("Observation?code=http://loinc.org|29463-7"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString11() throws IOException {

    String simplestProgram = "Patient/example";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected = Arrays.asList(Arrays.asList("Patient/example"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString12() throws IOException {

    String simplestProgram =
        "Coverage?type=http%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C1%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C11%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C111%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C112%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C113%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C119%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C12%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C121%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C122%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C123%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C129%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C13%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C14%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C19%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C191%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C2%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C21%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C211%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C212%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C213%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C219%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C22%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C23%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C25%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C26%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C29%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C291%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C299%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C31%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C311%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3111%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3112%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3113%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3114%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3115%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3116%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3119%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C312%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3121%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3122%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3123%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C313%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C321%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3211%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3212%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32121%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32122%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32123%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32124%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32125%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32126%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32127%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C32128%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C322%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3221%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3222%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3223%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C3229%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C33%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C331%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C332%2Chttp%3A%2F%2Fwww.phdsc.org%2Fstandards%2Fpdfs%2FSourceofPaymentTypologyVersion6FINALSeptember2015.pdf%7C333&policy-holder=Patient%2Fexample";
    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Coverage?type=http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|1,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|11,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|111,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|112,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|113,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|119,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|12,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|121,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|122,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|123,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|129,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|13,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|14,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|19,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|191,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|2,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|21,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|211,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|212,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|213,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|219,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|22,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|23,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|25,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|26,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|29,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|291,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|299,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|31,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|311,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3111,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3112,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3113,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3114,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3115,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3116,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3119,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|312,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3121,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3122,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3123,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|313,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|321,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3211,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3212,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32121,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32122,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32123,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32124,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32125,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32126,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32127,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|32128,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|322,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3221,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3222,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3223,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|3229,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|33,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|331,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|332,http://www.phdsc.org/standards/pdfs/SourceofPaymentTypologyVersion6FINALSeptember2015.pdf|333"),
            Arrays.asList(
                "Coverage?policy-holder.identifier={Patient.identifier}", "Patient?_id=example"));

    assertEquals(expected, actual);
  }

  @Test
  public void testExploratoryString13() throws IOException {

    String simplestProgram = "Patient?identifier1%7C2&_revinclude=Coverage:subscriber";
    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Patient?identifier1|2"),
            Arrays.asList("Coverage?subscriber.identifier={Patient.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testRevInclude() throws IOException {

    String simplestProgram = "Patient?_revinclude=Encounter:subject";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Patient"),
            Arrays.asList("Encounter?subject.identifier={Patient.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testInclude2() throws IOException {

    String simplestProgram = "Patient?_revinclude=EpisodeOfCare:patient";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Patient"),
            Arrays.asList("EpisodeOfCare?patient.identifier={Patient.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testIterate() throws IOException {

    String simplestProgram =
        "MedicationDispense?_include=MedicationDispense:prescription&_include:iterate=MedicationRequest:performer";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("MedicationDispense"),
            Arrays.asList("null?identifier={MedicationDispense.prescription.identifier}"),
            Arrays.asList("null?identifier={MedicationRequest.performer.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testRevInclude1() throws IOException {

    String simplestProgram = "Patient?name=Hodges&_revinclude=Encounter:subject";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Patient?name=Hodges"),
            Arrays.asList("Encounter?subject.identifier={Patient.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testRevInclude2() throws IOException {

    String simplestProgram =
        "Patient?_revinclude=Encounter:subject&identifier=http://hl7.org/fhir/sid/us-ssn|999622736";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Patient?identifier=http://hl7.org/fhir/sid/us-ssn|999622736"),
            Arrays.asList("Encounter?subject.identifier={Patient.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testInclude() throws IOException {

    String simplestProgram = "MedicationRequest?_include=MedicationRequest:patient";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("MedicationRequest"),
            Arrays.asList("null?identifier={MedicationRequest.patient.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testTypedInclude() throws IOException {

    String simplestProgram = "MedicationRequest?_include=MedicationRequest:patient:Patient";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("MedicationRequest"),
            Arrays.asList("Patient?identifier={MedicationRequest.patient.identifier}"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage0() throws IOException {

    String simplestProgram =
        "Observation?patient.identifier=http://example.com/fhir/identifier/mrn|123456";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "Observation?patient.identifier=http://example.com/fhir/identifier/mrn|123456"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage1() throws IOException {

    String simplestProgram = "Patient?_id=23";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected = Arrays.asList(Arrays.asList("Patient?_id=23"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage2() throws IOException {

    String simplestProgram = "Patient/23";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected = Arrays.asList(Arrays.asList("Patient/23"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage3() throws IOException {

    String simplestProgram = "Observation?_lastUpdated=gt2010-10-01";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(Arrays.asList("Observation?_lastUpdated=gt2010-10-01"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage4() throws IOException {

    String simplestProgram = "Condition?_tag=http://acme.org/codes|needs-review";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(Arrays.asList("Condition?_tag=http://acme.org/codes|needs-review"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage5() throws IOException {

    String simplestProgram =
        "DiagnosticReport?_profile=http://hl7.org/fhir/StructureDefinition/lipid";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList(
                "DiagnosticReport?_profile=http://hl7.org/fhir/StructureDefinition/lipid"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage6() throws IOException {

    String simplestProgram = "DiagnosticReport?_profile=Profile/lipid";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(Arrays.asList("DiagnosticReport?_profile=Profile/lipid"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage7() throws IOException {

    String simplestProgram = "RiskAssessment?probability=gt0.8";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected = Arrays.asList(Arrays.asList("RiskAssessment?probability=gt0.8"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage8() throws IOException {

    String simplestProgram = "Patient?identifier:otype=http://terminology.hl7.org/CodeSystem/v2-0203|MR|446053";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Patient?identifier:otype=http://terminology.hl7.org/CodeSystem/v2-0203|MR|446053"));

    assertEquals(expected, actual);
  }

  

  @Test
  public void testSearchPage9() throws IOException {

    String simplestProgram = "Condition?code:in=http%3A%2F%2Fsnomed.info%2Fsct%3Ffhir_vs%3Disa%2F126851005";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Condition?code:in=http://snomed.info/sct?fhir_vs=isa/126851005"));

    assertEquals(expected, actual);
  }


  @Test
  public void testSearchPage10() throws IOException {

    String simplestProgram = "Observation?value-quantity=5.40e-3|http://unitsofmeasure.org|g";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Observation?value-quantity=5.40e-3|http://unitsofmeasure.org|g"));

    assertEquals(expected, actual);
  }

  @Test
  public void testSearchPage11() throws IOException {

    String simplestProgram = "Observation?value-quantity=5.4||mg";

    List<List<String>> actual = toUrls(simplestProgram);
    System.out.println(actual);

    List<List<String>> expected =
        Arrays.asList(
            Arrays.asList("Observation?value-quantity=5.4||mg"));

    assertEquals(expected, actual);
  }

  
  

  private List<List<String>> toUrls(String simplestProgram) throws IOException {
    FhirUrlAnalyser visitor = toVisitor(simplestProgram);

    List<List<ParsedUrl>> perAndParameter =
        visitor.getAndParameters().stream()
            .map(
                httpParam ->
                    visitor.getResourcesForHttpParam(false, httpParam).stream()
                        .map(
                            resourceInParam ->
                                new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
                        .flatMap(
                            opt ->
                                opt.isPresent()
                                    ? Arrays.<ParsedUrl>asList(opt.get()).stream()
                                    : Stream.<ParsedUrl>empty())
                        .collect(Collectors.toList()))
            .collect(Collectors.toList());

    List<List<ParsedUrl>> perIncludeParameter =
        visitor.getIncludeParameters().stream()
            .map(
                httpParam ->
                    visitor.getResourcesForHttpParam(true, httpParam).stream()
                        .map(
                            resourceInParam ->
                                new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
                        .flatMap(
                            opt ->
                                opt.isPresent()
                                    ? Arrays.<ParsedUrl>asList(opt.get()).stream()
                                    : Stream.<ParsedUrl>empty())
                        .collect(Collectors.toList()))
            .collect(Collectors.toList());

    List<List<String>> retVal = new ArrayList<>();
    retVal.addAll(
        perAndParameter.stream()
            .map(param -> param.stream().map(x -> x.toString()).collect(Collectors.toList()))
            .collect(Collectors.toList()));
    retVal.addAll(
        perIncludeParameter.stream()
            .map(param -> param.stream().map(x -> x.toString()).collect(Collectors.toList()))
            .collect(Collectors.toList()));

    return retVal;
  }

  public static FhirUrlAnalyser toVisitor(String simplestProgram) throws IOException {
    simplestProgram = URLDecoder.decode(simplestProgram, "UTF-8");
    CharStream inputCharStream = CharStreams.fromReader(new StringReader(simplestProgram));
    TokenSource tokenSource = new FhirUrlLexer(inputCharStream);
    TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
    FhirUrlParser parser = new FhirUrlParser(inputTokenStream);
    SContext context = parser.s();

    ourLog.info(TreeUtils.toPrettyTree(context, Arrays.asList(parser.getRuleNames())));

    ourLog.info(context.getText());

    FhirUrlAnalyser visitor = new FhirUrlAnalyser();

    context.accept(visitor);
    ourLog.info(visitor.getResources().toString());
    ourLog.info(visitor.getHttpParams().toString());
    return visitor;
  }
}
