package com.historias_clinicas.hc.servicios.word;

import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JodConverterConfig {

    @Bean(destroyMethod = "stop")
    public OfficeManager officeManager() throws OfficeException {

        LocalOfficeManager manager = LocalOfficeManager.builder()
                .officeHome("C:/Program Files/LibreOffice")
                .portNumbers(2005) // 👈 PUERTO NUEVO
                .install()
                .build();

        manager.start();
        return manager;
    }


    @Bean
    public LocalConverter localConverter(OfficeManager officeManager) {
        return LocalConverter.make(officeManager);
    }
}
