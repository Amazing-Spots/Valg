package com.runtimeverification.rvmonitor.java.rvj.output;

import java.util.TreeMap;

import com.runtimeverification.rvmonitor.java.rvj.output.combinedoutputcode.CombinedOutput;
import com.runtimeverification.rvmonitor.java.rvj.output.combinedoutputcode.indexingtree.reftree.RefTree;
import com.runtimeverification.rvmonitor.java.rvj.output.monitor.MonitorFeatures;
import com.runtimeverification.rvmonitor.java.rvj.output.monitor.SuffixMonitor;
import com.runtimeverification.rvmonitor.java.rvj.output.monitorset.MonitorSet;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.RVMSpecFile;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.rvmspec.PropertyAndHandlers;
import com.runtimeverification.rvmonitor.java.rvj.parser.ast.rvmspec.RVMonitorSpec;
import com.runtimeverification.rvmonitor.util.RVMException;

public class RVMOutputCode {

    private final Package packageDecl;
    private final Imports imports;
    private final TreeMap<RVMonitorSpec, MonitorSet> monitorSets = new TreeMap<>();
    private final TreeMap<RVMonitorSpec, SuffixMonitor> monitors = new TreeMap<>();
    private final CombinedOutput output;
    private boolean isCodeGenerated = false;

    public RVMOutputCode(String name, RVMSpecFile rvmSpecFile) throws RVMException {
        packageDecl = new Package(rvmSpecFile);
        imports = new Imports(rvmSpecFile);

        TreeMap<RVMonitorSpec, EnableSet> enableSets = new TreeMap<>();
        for (RVMonitorSpec rvmSpec : rvmSpecFile.getSpecs()) {
            EnableSet enableSet = new EnableSet(rvmSpec.getEvents(), rvmSpec.getParameters());
            CoEnableSet coenableSet = new CoEnableSet(rvmSpec.getEvents(), rvmSpec.getParameters());

            for (PropertyAndHandlers prop : rvmSpec.getPropertiesAndHandlers()) {
                enableSet.add(new EnableSet(prop, rvmSpec.getEvents(), rvmSpec.getParameters()));
                coenableSet.add(new CoEnableSet(prop, rvmSpec.getEvents(), rvmSpec.getParameters()));
            }

            OptimizedCoenableSet optimizedCoenableSet = new OptimizedCoenableSet(coenableSet);

            enableSets.put(rvmSpec, enableSet);

            SuffixMonitor monitor = new SuffixMonitor(rvmSpec.getName(), rvmSpec, optimizedCoenableSet, true);

            monitors.put(rvmSpec, monitor);

            monitorSets.put(rvmSpec, new MonitorSet(rvmSpec, monitor));
        }

        output = new CombinedOutput(name, rvmSpecFile, monitorSets, monitors, enableSets);

        // Set monitor lock for each monitor set
        for (MonitorSet monitorSet : monitorSets.values()) {
            monitorSet.setMonitorLock(output.getName() + "." + output.lockManager.getLock(monitorSet.getRVMSpecName()).getName());
            monitorSet.setIndexingTreeManager(output.indexingTreeManager);
        }

        TreeMap<String, RefTree> refTrees = output.indexingTreeManager.refTrees;

        for (SuffixMonitor monitor : monitors.values()) {
            monitor.setRefTrees(refTrees);
        }
    }

    public void generateCode() {
        if (!this.isCodeGenerated) {
            this.output.generateCode();
        }

        this.postprocessMonitorFeatures();

        this.isCodeGenerated = true;
    }

    private void postprocessMonitorFeatures() {
        for (SuffixMonitor monitor : this.monitors.values()) {
            MonitorFeatures features = monitor.getFeatures();
            features.onCodeGenerationPass1Completed();
        }
    }

    @Override
    public String toString() {
        // Some non-trivial and potentially side-effect generating works should be done.
        // Since toString() is not a very nice place to do such things, I created a
        // method and let is invoked. I hope such changes are made in many other places.
        this.generateCode();

        StringBuilder ret = new StringBuilder();

        ret.append(packageDecl);
        ret.append(imports);
        ret.append("\n");

        for (MonitorSet monitorSet : this.monitorSets.values())
            ret.append(monitorSet);
        ret.append("\n");

        for (SuffixMonitor monitor : this.monitors.values())
            ret.append(monitor);
        ret.append("\n");

        ret.append(output);

        return ret.toString();
    }
}
