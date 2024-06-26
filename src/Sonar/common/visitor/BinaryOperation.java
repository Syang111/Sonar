package Sonar.common.visitor;

public interface BinaryOperation<T> {

    T getLeft();

    T getRight();

    String getOperatorRepresentation();
}
