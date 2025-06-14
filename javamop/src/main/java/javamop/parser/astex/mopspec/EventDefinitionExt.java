// Copyright (c) 2002-2014 JavaMOP Team. All Rights Reserved.
package javamop.parser.astex.mopspec;

import java.io.ByteArrayInputStream;
import java.util.List;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import javamop.parser.ast.aspectj.PointCut;
import javamop.parser.ast.aspectj.TypePattern;
import javamop.parser.ast.mopspec.MOPParameter;
import javamop.parser.ast.mopspec.MOPParameters;
import javamop.parser.ast.visitor.BaseVisitor;
import javamop.parser.ast.visitor.MOPVoidVisitor;
import javamop.parser.astex.ExtNode;
import javamop.parser.astex.visitor.DumpVisitor;

public class EventDefinitionExt extends ExtNode {

    private final String id;
    private final Type retType;
    private final String pos;

    private boolean abstractEvent = false;
    private final String pointCutStr;
    private String purePointCutStr;
    private PointCut pointCut;

    private final MOPParameters parameters;
    private final boolean hasReturning;
    private final MOPParameters retVal;
    private final boolean hasThrowing;
    private final MOPParameters throwVal;

    final MOPParameters mopParameters;

    private final BlockStmt block;

    // will be modified by JavaMOPSpec when creation events are not specified
    boolean startEvent = false;
    
    // similar to startEvent, but won't be modified
    private final boolean creationEvent;
    
    private final boolean blockingEvent;

    private String condition;
    private String threadVar;
    private TypePattern endObjectType;
    private String endObjectId;
    private boolean endProgram = false;
    private boolean endThread = false;
    private boolean startThread = false;
    private boolean endObject = false;
    private boolean staticEvent = false;

    // things that should be defined afterward
    int idnum; // will be defined in JavaMOPSpec
    boolean duplicated = false; // will be defined in JavaMOPSpec
    String uniqueId = null; // will be defined in JavaMOPSpec
    MOPParameters mopParametersOnSpec; // will be defined in JavaMOPSpec

    private MOPParameters parametersWithoutThreadVar = null;

    public EventDefinitionExt(TokenRange tokenRange, String id, Type retType, String pos, List<MOPParameter> parameters, String pointCutStr, BlockStmt block, boolean hasReturning,
                              List<MOPParameter> retVal, boolean hasThrowing, List<MOPParameter> throwVal, boolean startEvent, boolean abstractEvent,
                              boolean isBlockingEvent, boolean staticEvent)
            throws com.github.javaparser.ParseException {
        super(tokenRange);
        this.id = id;
        this.retType = retType;
        this.pos = pos;
        this.parameters = new MOPParameters(parameters);
        this.pointCutStr = pointCutStr;
        this.block = block;
        this.hasReturning = hasReturning;
        this.retVal = new MOPParameters(retVal);
        this.hasThrowing = hasThrowing;
        this.throwVal = new MOPParameters(throwVal);
        
        if (pointCutStr != null) {
            this.pointCut = parsePointCutAsRaw(pointCutStr);
        }
        this.startEvent = startEvent;
        this.creationEvent = startEvent;
        this.mopParameters = new MOPParameters();
        this.mopParameters.addAll(this.parameters);
        this.mopParameters.addAll(this.retVal);
        this.mopParameters.addAll(this.throwVal);
        this.abstractEvent = abstractEvent;
        this.blockingEvent = isBlockingEvent;
        this.staticEvent = staticEvent;
    }

    public EventDefinitionExt(TokenRange tokenRange, EventDefinitionExt e) {
        super(tokenRange);
        this.id = e.getId();
        this.retType = e.getRetType();
        this.pos = e.getPos();
        this.parameters = e.getParameters();
        this.pointCutStr = e.getPointCutString();
        this.block = e.getBlock();
        this.hasReturning = e.getHasRetruning();
        this.retVal = e.getRetVal();
        this.hasThrowing = e.getHasThrowing();
        this.throwVal = e.getThrowVal();
        this.pointCut = e.getPointCut();
        this.startEvent = e.isStartEvent();
        this.creationEvent = e.isCreationEvent();
        this.blockingEvent = e.isBlockingEvent();
        this.mopParameters = e.getMOPParameters();
        this.condition = e.getCondition();
        this.threadVar = e.getThreadVar();
        this.endObjectType = e.getEndObjectType();
        this.endObjectId = e.getEndObjectId();

        this.endProgram = e.isEndProgram();
        this.endThread = e.isEndThread();
        this.endObject = e.isEndObject();

        this.idnum = e.getIdNum(); // will be defined in JavaMOPSpec
        this.duplicated = e.isDuplicated(); // will be defined in JavaMOPSpec
        this.uniqueId = e.getUniqueId(); // will be defined in JavaMOPSpec
        this.mopParametersOnSpec = e.getMOPParametersOnSpec(); // will be
                                                                // defined in
                                                                // JavaMOPSpec

        this.parametersWithoutThreadVar = e.getParametersWithoutThreadVar();
    }

    private PointCut parsePointCutAsRaw(String input) throws com.github.javaparser.ParseException {
        // create a token for exceptions
//        com.github.javaparser.Token t = new com.github.javaparser.Token();
//        t.beginLine = getTokenRange().get().getBegin().getRange().get().begin.line;
//        t.beginColumn = getTokenRange().get().getBegin().getRange().get().begin.column;

        PointCut originalPointCut;
        PointCut resultPointCut;
        purePointCutStr = "";
        threadVar = "";
        condition = "";

        try {
            originalPointCut = com.github.javaparser.AspectJParser.parse(new ByteArrayInputStream(input.getBytes()));
        } catch (com.github.javaparser.ParseException e) {
            throw new com.github.javaparser.ParseException("The following error encountered when parsing the pointcut in the event definition: " + e.getMessage());
        }

        resultPointCut = originalPointCut;

        if (resultPointCut == null)
            throw new com.github.javaparser.ParseException("endObject() pointcut should appear at the root level in a conjunction form");

        purePointCutStr = resultPointCut.toRVString();

        return resultPointCut;
    }

    public String getId() {
        return id;
    }

    public int getIdNum() {
        return idnum;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Type getRetType() {
        return retType;
    }

    public String getPos() {
        return pos;
    }

    public MOPParameters getParameters() {
        return parameters;
    }

    public MOPParameters getParametersWithoutThreadVar() {
        if (parametersWithoutThreadVar != null)
            return parametersWithoutThreadVar;

        parametersWithoutThreadVar = new MOPParameters();
        for (MOPParameter param : parameters) {
            if (getThreadVar() != null && getThreadVar().length() != 0 && param.getName().equals(getThreadVar()))
                continue;
            parametersWithoutThreadVar.add(param);
        }

        return parametersWithoutThreadVar;
    }

    public MOPParameters getMOPParameters() {
        return mopParameters;
    }

    MOPParameters mopParametersWithoutThreadVar = null;

    public MOPParameters getMOPParametersWithoutThreadVar() {
        if (mopParametersWithoutThreadVar != null)
            return mopParametersWithoutThreadVar;

        mopParametersWithoutThreadVar = new MOPParameters();
        for (MOPParameter param : mopParameters) {
            if (getThreadVar() != null && getThreadVar().length() != 0 && param.getName().equals(getThreadVar()))
                continue;
            mopParametersWithoutThreadVar.add(param);
        }
        return mopParametersWithoutThreadVar;
    }

    public MOPParameters getMOPParametersOnSpec() {
        return mopParametersOnSpec;
    }

    public String getPointCutString() {
        return pointCutStr;
    }

    public BlockStmt getAction() {
        return block;
    }

    public boolean hasReturning() {
        return hasReturning;
    }

    public MOPParameters getRetVal() {
        return retVal;
    }

    public boolean hasThrowing() {
        return hasThrowing;
    }

    public MOPParameters getThrowVal() {
        return throwVal;
    }

    public String getThreadVar() {
        return threadVar;
    }

    public String getCondition() {
        return condition;
    }

    public boolean isAbstract() {
        return this.abstractEvent;
    }

    public String getPurePointCutString() {
        return purePointCutStr;
    }

    public String getEndObjectVar() {
        if (this.endObject)
            return endObjectId;
        else
            return null;
    }

    public TypePattern getEndObjectType() {
        if (this.endObject)
            return endObjectType;
        else
            return null;
    }

    public boolean isStartEvent() {
        return this.startEvent;
    }
    
    public boolean isBlockingEvent() {
        return this.blockingEvent;
    }
    
    public boolean isCreationEvent() {
        return this.creationEvent;
    }

    public boolean isEndProgram() {
        return this.endProgram;
    }

    public boolean isEndThread() {
        return this.endThread;
    }

    public boolean isEndObject() {
        return this.endObject;
    }

    public boolean isStaticEvent() {
        return staticEvent;
    }

    public BlockStmt getBlock() {
        return this.block;
    }

    public boolean getHasRetruning() {
        return this.hasReturning;
    }

    public boolean getHasThrowing() {
        return this.hasThrowing;
    }

    public PointCut getPointCut() {
        return this.pointCut;
    }

    public String getEndObjectId() {
        return this.endObjectId;
    }

    public boolean isDuplicated() {
        return this.duplicated;
    }
    
    public boolean isImplementing(EventDefinitionExt absEvent){
        if (this.isAbstract())
            return false;
        if (!this.getId().equals(absEvent.getId()))
            return false;
        if (this.getParameters().matchTypes(absEvent.getParameters()))
            return false;
        if (this.getRetVal().matchTypes(absEvent.getRetVal()))
            return false;
        if (this.getThrowVal().matchTypes(absEvent.getThrowVal()))
            return false;
        if (!this.getPos().equals(absEvent.getPos()))
            return false;

        return true; 
    }

    public <A> void accept(VoidVisitor<A> v, A arg) {
        if (v instanceof javamop.parser.astex.visitor.DumpVisitor) {
            ((DumpVisitor)v).visit(this, (Void) arg);
	} else if (v instanceof javamop.parser.ast.visitor.MOPVoidVisitor) {
            ((MOPVoidVisitor)v).visit(this, arg);
        }
    }

    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        if (v instanceof BaseVisitor) {
            return ((BaseVisitor<R, A>) v).visit(this, arg);
        } else {
            return null;
        }
    }
}
