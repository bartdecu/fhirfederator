package ca.uhn.fhir.federator;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ca.uhn.fhir.federator.FhirUrlParser.SContext;

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
        public void syntaxError(Recognizer<?, ?> arg0, Object arg1, int arg2,
                int arg3, String arg4, RecognitionException arg5) {
            setFail(true);
        }

        @Override
        public void reportContextSensitivity(Parser arg0, DFA arg1, int arg2,
                int arg3, int arg4, ATNConfigSet arg5) {
            setFail(true);
        }

        @Override
        public void reportAttemptingFullContext(Parser arg0, DFA arg1, int arg2,
                int arg3, BitSet arg4, ATNConfigSet arg5) {
            setFail(true);
        }

        @Override
        public void reportAmbiguity(Parser arg0, DFA arg1, int arg2, int arg3,
                boolean arg4, BitSet arg5, ATNConfigSet arg6) {
            setFail(true);
        }
    }

    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TestFhirUrlParser.class);

    @Test
    public void testExploratoryString() throws IOException {

        String simplestProgram = "Patient?_has:Observation:patient:_has:AuditEvent:entity:agent:Practitioner.name=janedoe";

        List<List<String>> actual = toUrls(simplestProgram);

        List<List<String>> expected = Arrays.asList(Arrays.asList("Patient?identifier={Observation.patient.identifier}",
                "Observation?identifier={AuditEvent.entity.identifier}",
                "AuditEvent?agent.identifier={Practitioner.identifier}", "Practitioner?name=janedoe"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString1() throws IOException {

        String simplestProgram = "DiagnosticReport?subject:Patient.name=Sarah";

        List<List<String>> actual = toUrls(simplestProgram);

        List<List<String>> expected = Arrays.asList(Arrays.asList("DiagnosticReport?subject.identifier={Patient.identifier}",
                "Patient?name=Sarah"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString2() throws IOException {

        String simplestProgram = "Encounter?subject=Patient/78a14cbe-8968-49fd-a231-d43e6619399f";

        List<List<String>> actual = toUrls(simplestProgram);

        List<List<String>> expected = Arrays.asList(Arrays.asList("Encounter?subject.identifier={Patient.identifier}",
                "Patient?_id=78a14cbe-8968-49fd-a231-d43e6619399f"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString3() throws IOException {

        String simplestProgram = "Encounter?subject:Patient.birthdate=1987-02-20";

        List<List<String>> actual = toUrls(simplestProgram);

        List<List<String>> expected =Arrays.asList(Arrays.asList("Encounter?subject.identifier={Patient.identifier}",
                "Patient?birthdate=1987-02-20"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString4() throws IOException {

        String simplestProgram = "Patient?birthdate:missing=true";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList("Patient?birthdate:missing=true"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString5() throws IOException {

        String simplestProgram = "Patient?general-practitioner:Practitioner.name=Sarah&general-practitioner:Practitioner.address-state=WA";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList( Arrays.asList("Patient?general-practitioner.identifier={Practitioner.identifier}",
                "Practitioner?name=Sarah"),Arrays.asList("Patient?general-practitioner.identifier={Practitioner.identifier}",
                "Practitioner?address-state=WA"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString6() throws IOException {

        String simplestProgram = "Patient?identifier=https://github.com/synthetichealth/synthea|621338a9-01f4-49d4-b852-14507a8bf8c7";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList(
                "Patient?identifier=https://github.com/synthetichealth/synthea|621338a9-01f4-49d4-b852-14507a8bf8c7"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString7() throws IOException {

        String simplestProgram = "Patient?name=Sarah&name=Jones";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList(
                "Patient?name=Sarah"),Arrays.asList( "Patient?name=Jones"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testExploratoryString8() throws IOException {

        String simplestProgram = "Observation?subject.identifier=urn:oid:1.2.36.146.595.217.0.1|12345";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList(
                "Observation?subject.identifier=urn:oid:1.2.36.146.595.217.0.1|12345"));

        Assert.assertEquals(expected, actual);

    }

    

    @Test
    public void testExploratoryString9() throws IOException {

        String simplestProgram = "Observation?subject:Patient.name=Hodges&code=http://loinc.org|29463-7";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList(
            "Observation?subject.identifier={Patient.identifier}", "Patient?name=Hodges"), Arrays.asList( "Observation?code=http://loinc.org|29463-7"));

        Assert.assertEquals(expected, actual);

    }


    

    // TODO _include and _revinclude are todo
    @Test
    public void testRevInclude() throws IOException {

        String simplestProgram = "Patient?_revinclude=Encounter:subject";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList("Patient"),Arrays.asList("Encounter?subject={Patient.identifier}"));

        Assert.assertEquals(expected, actual);

    }


    @Test
    public void testRevInclude1() throws IOException {

        String simplestProgram = "Patient?name=Hodges&_revinclude=Encounter:subject";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList("Patient?name=Hodges"),Arrays.asList("Encounter?subject={Patient.identifier}"));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testRevInclude2() throws IOException {

        String simplestProgram = "Patient?_revinclude=Encounter:subject&identifier=http://hl7.org/fhir/sid/us-ssn|999622736";

        List<List<String>> actual = toUrls(simplestProgram);
        System.out.println(actual);

        List<List<String>> expected = Arrays.asList(Arrays.asList("Patient?identifier=http://hl7.org/fhir/sid/us-ssn|999622736"),Arrays.asList("Encounter?subject={Patient.identifier}"));

        Assert.assertEquals(expected, actual);

    }

    //Patient?_revinclude=Encounter:subject&identifier=http://hl7.org/fhir/sid/us-ssn|999622736

    private List<List<String>> toUrls(String simplestProgram) throws IOException {
        FhirUrlAnalyser visitor = toVisitor(simplestProgram);

        List<List<ParsedUrl>> perAndParameter = visitor.getAndParameters().stream()
                .map(httpParam -> visitor.getResourcesForHttpParam(httpParam).stream()
                        .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
                        .flatMap(opt -> opt.isPresent()?Arrays.<ParsedUrl>asList(opt.get()).stream():Stream.<ParsedUrl>empty())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

                List<List<ParsedUrl>> perIncludeParameter;
                if (perAndParameter.isEmpty()){
            perAndParameter = visitor.getIncludeParameters().stream()
                .map(httpParam -> visitor.getResourcesForHttpParam(httpParam).subList(0,1).stream()
                        .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
                        .flatMap(opt -> opt.isPresent()?Arrays.<ParsedUrl>asList(opt.get()).stream():Stream.<ParsedUrl>empty())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        } 
            perIncludeParameter = visitor.getIncludeParameters().stream()
                .map(httpParam -> visitor.getResourcesForHttpParam(httpParam).subList(1,visitor.getResourcesForHttpParam(httpParam).size()).stream()
                        .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
                        .flatMap(opt -> opt.isPresent()?Arrays.<ParsedUrl>asList(opt.get()).stream():Stream.<ParsedUrl>empty())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        


        List<List<String>> retVal = new ArrayList<>();
        retVal.addAll(perAndParameter.stream().map(param -> param.stream().map(x->x.toString()).collect(Collectors.toList())).collect(Collectors.toList()));
        retVal.addAll(perIncludeParameter.stream().map(param -> param.stream().map(x->x.toString()).collect(Collectors.toList())).collect(Collectors.toList()));
        

        return retVal;
    }

    public static FhirUrlAnalyser toVisitor(String simplestProgram) throws IOException {
        CharStream inputCharStream = new ANTLRInputStream(new StringReader(simplestProgram));
        TokenSource tokenSource = new FhirUrlLexer(inputCharStream);
        TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
        FhirUrlParser parser = new FhirUrlParser(inputTokenStream);
        SContext context = parser.s();

        ourLog.info(TreeUtils.toPrettyTree(context,Arrays.asList(parser.getRuleNames())));

        ourLog.info(context.getText());

        FhirUrlAnalyser visitor = new FhirUrlAnalyser();

        context.accept(visitor);
        ourLog.info(visitor.getResources().toString());
        ourLog.info(visitor.getHttpParams().toString());
        return visitor;
    }

}
