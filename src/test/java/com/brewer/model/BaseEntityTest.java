package com.brewer.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseEntityTest {

    @Test
    public void shouldImplementEqualsAndHashCodeBasedOnCodigo() {
        TestEntity first = new TestEntity();
        first.setCodigo(1L);

        TestEntity second = new TestEntity();
        second.setCodigo(1L);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    public void shouldTreatNullCodigoAsEqualForSameConcreteClass() {
        assertThat(new TestEntity()).isEqualTo(new TestEntity());
    }

    @Test
    public void shouldNotBeEqualToDifferentConcreteSubclass() {
        TestEntity first = new TestEntity();
        first.setCodigo(1L);

        OtherTestEntity second = new OtherTestEntity();
        second.setCodigo(1L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    public void shouldExposeNonEmptyInheritedToStringForConcreteSubclass() {
        assertThat(new TestEntity().toString()).contains("TestEntity");
    }

    private static class TestEntity extends BaseEntity {
        private static final long serialVersionUID = 1L;
    }

    private static class OtherTestEntity extends BaseEntity {
        private static final long serialVersionUID = 1L;
    }
}
