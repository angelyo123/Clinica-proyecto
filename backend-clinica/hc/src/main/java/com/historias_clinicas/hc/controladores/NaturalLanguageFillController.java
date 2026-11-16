package com.historias_clinicas.hc.controladores;

import com.historias_clinicas.hc.extraction.dto.FieldValueAI;
import com.historias_clinicas.hc.extraction.dto.NaturalLanguageFillRequest;
import com.historias_clinicas.hc.extraction.service.NaturalLanguageFillService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fill")
public class NaturalLanguageFillController {

    private final NaturalLanguageFillService fillService;

    public NaturalLanguageFillController(NaturalLanguageFillService fillService) {
        this.fillService = fillService;
    }

    @PostMapping
    public List<FieldValueAI> fill(
            @RequestBody NaturalLanguageFillRequest request) {

        return fillService.process(
                request.getTexto(),
                request.getCampos()
        );
    }
}