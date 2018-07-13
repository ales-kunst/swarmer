package org.swarmer.context;

public interface CtxElementVisitor<RT> {

    void visit(SwarmerCtx ctx) throws Exception; // NOSONAR

    void visit(DeploymentContainer container) throws Exception; // NOSONAR

    void visit(SwarmDeployment deployment) throws Exception; // NOSONAR

    RT getData();
}
