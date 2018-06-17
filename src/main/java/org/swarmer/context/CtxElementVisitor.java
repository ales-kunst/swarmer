package org.swarmer.context;

public interface CtxElementVisitor<RT> {

    void visit(SwarmerCtx ctx) throws Exception;

    void visit(DeploymentContainer container) throws Exception;

    void visit(SwarmDeployment deployment)  throws Exception;

    RT getData();
}
