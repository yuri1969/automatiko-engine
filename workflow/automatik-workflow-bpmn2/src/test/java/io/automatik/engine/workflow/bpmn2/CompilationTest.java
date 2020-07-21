package io.automatik.engine.workflow.bpmn2;//

//package org.jbpm.bpmn2;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.drools.compiler.compiler.AnalysisResult;
//import org.drools.compiler.compiler.ReturnValueDescr;
//import org.drools.compiler.lang.descr.ActionDescr;
//import org.drools.compiler.lang.descr.BaseDescr;
//import org.drools.compiler.rule.builder.PackageBuildContext;
//import org.jbpm.process.builder.ActionBuilder;
//import org.jbpm.process.builder.ReturnValueEvaluatorBuilder;
//import org.jbpm.process.builder.dialect.ProcessDialectRegistry;
//import org.jbpm.process.builder.dialect.java.JavaActionBuilder;
//import org.jbpm.process.builder.dialect.java.JavaProcessDialect;
//import org.jbpm.process.builder.dialect.java.JavaReturnValueEvaluatorBuilder;
//import org.jbpm.process.core.ContextResolver;
//import org.jbpm.process.instance.impl.ReturnValueConstraintEvaluator;
//import org.jbpm.workflow.core.DroolsAction;
//import org.junit.jupiter.api.Test;
//import org.kie.api.KieBase;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//public class CompilationTest extends JbpmBpmn2TestCase {
//
//    @Test
//    public void testReturnValueDescrCreation() throws Exception {
//        TestJavaProcessDialect javaProcessDialect = new TestJavaProcessDialect();
//        ProcessDialectRegistry.setDialect("java", javaProcessDialect);
//
//        String filename = "BPMN2-GatewaySplit-SequenceConditions.bpmn2";
//        KieBase kbase = createKnowledgeBase(filename);
//
//        assertFalse(javaProcessDialect.getActionDescrs().isEmpty(),
//                    "No " + ActionDescr.class.getSimpleName() + " instances caught for testing!");
//        for( BaseDescr descr : javaProcessDialect.getActionDescrs() ) {
//            assertNotNull(descr.getResource(), descr.getClass().getSimpleName() +" has a null resource field");
//        }
//
//        assertFalse(javaProcessDialect.getReturnValueDescrs().isEmpty(),
//                    "No " + ReturnValueDescr.class.getSimpleName() + " instances caught for testing!");
//        for( BaseDescr descr : javaProcessDialect.getReturnValueDescrs() ) {
//            assertNotNull(descr.getResource(), descr.getClass().getSimpleName() + " has a null resource field");
//        }
//    }
//
//    private static class TestJavaProcessDialect extends JavaProcessDialect {
//
//        private ActionBuilder actionBuilder = new TestJavaActionBuilder();
//        private ReturnValueEvaluatorBuilder returnValueEvaluatorBuilder = new TestJavaReturnValueEvaluatorBuilder();
//
//        @Override
//        public ActionBuilder getActionBuilder() {
//            return actionBuilder;
//        }
//
//        @Override
//        public ReturnValueEvaluatorBuilder getReturnValueEvaluatorBuilder() {
//            return returnValueEvaluatorBuilder;
//        }
//
//        public List<ActionDescr> getActionDescrs() {
//            return ((TestJavaActionBuilder) actionBuilder).actionDescrs;
//        }
//
//        public List<ReturnValueDescr> getReturnValueDescrs() {
//            return ((TestJavaReturnValueEvaluatorBuilder) returnValueEvaluatorBuilder).returnValueDescrs;
//        }
//    }
//
//    private static class TestJavaActionBuilder extends JavaActionBuilder {
//
//        List<ActionDescr> actionDescrs = new ArrayList<ActionDescr>();
//
//        @Override
//        protected void buildAction( PackageBuildContext context, DroolsAction action, ActionDescr actionDescr,
//                ContextResolver contextResolver, String className, AnalysisResult analysis ) {
//            actionDescrs.add(actionDescr);
//            super.buildAction(context, action, actionDescr, contextResolver, className, analysis);
//        }
//    }
//
//    private static class TestJavaReturnValueEvaluatorBuilder extends JavaReturnValueEvaluatorBuilder {
//
//        List<ReturnValueDescr> returnValueDescrs = new ArrayList<ReturnValueDescr>();
//
//        @Override
//        protected void buildReturnValueEvaluator( PackageBuildContext context, ReturnValueConstraintEvaluator constraintNode,
//                ReturnValueDescr descr, ContextResolver contextResolver, String className, AnalysisResult analysis ) {
//            returnValueDescrs.add(descr);
//            super.buildReturnValueEvaluator(context, constraintNode, descr, contextResolver, className, analysis);
//        }
//
//    }
//
//}
