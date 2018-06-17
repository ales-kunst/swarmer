package org.swarmer.context;

public interface CtxVisitableElement {
    void visit(CtxElementVisitor visitor) throws Exception;
}
