import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import soot.*;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

public class BodyPrintTransformer extends SceneTransformer
{
    /**
     * Print Android APIs
     */

    private String methodSig;
    private String outputPath;

    BodyPrintTransformer(String methodSig, String outputPath) {
        this.methodSig = methodSig;
        this.outputPath = outputPath;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options)
    {
        //(1) Obtain all application classes
        Chain<SootClass> sootClasses = Scene.v().getApplicationClasses();
        List<String> methodbody = new ArrayList<String>();

        for (Iterator<SootClass> iter = sootClasses.snapshotIterator(); iter.hasNext();)
        {
            SootClass sc = iter.next();
            if (!isSelfClass(sc)){
                continue;
            }

            String scClassName = sc.getName();
            System.out.println(scClassName);



            //(2) Obtain all the methods from a given class
            List<SootMethod> sootMethods = sc.getMethods();

            for (int i = 0; i < sootMethods.size(); i++) {
                SootMethod sm = sootMethods.get(i);
                String callerSig = sm.getSignature();
                if (!callerSig.equals(this.methodSig)){
                    continue;
                }

                try {
                    Body body = sm.retrieveActiveBody();

                    //(3) Obtain all statements from a given method
                    PatchingChain<Unit> units = body.getUnits();

                    for (Iterator<Unit> unitIter = units.snapshotIterator(); unitIter.hasNext(); ) {
                        Stmt stmt = (Stmt) unitIter.next();
                        methodbody.add(stmt.toString());
                    }
                } catch (Exception ex) {
                    //TODO: No active body retrieved from the method
                }
            }
        }

        try {
            this.Save(methodbody);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void Save(List<String> lst) throws Exception{
        FileOutputStream fos = new FileOutputStream(this.outputPath);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");

        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader("Line", "Body Stmt");
        CSVPrinter csvPrinter = new CSVPrinter(osw, csvFormat);

        int i = 1;
        for (String s : lst) {
            csvPrinter.printRecord(Integer.toString(i), s);
            i += 1;
        }
        csvPrinter.flush();
        csvPrinter.close();

    }

    private boolean isSelfClass(SootClass sootClass)
    {
        if (sootClass.isPhantom())
        {
            return false;
        }
        String packageName = sootClass.getPackageName();
        return this.methodSig.contains(packageName);
    }

}

