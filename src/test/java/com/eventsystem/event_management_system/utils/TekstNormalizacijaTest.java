package com.eventsystem.event_management_system.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TekstNormalizacijaTest {

    @Test
    void naziviSePodudaraju_ignoriseDijakritike() {
        assertThat(TekstNormalizacija.naziviSePodudaraju("Mikrofoni bezicni", "Mikrofoni bežični")).isTrue();
        assertThat(TekstNormalizacija.naziviSePodudaraju("Zvučnici PA", "Zvucnici PA")).isTrue();
    }
}
