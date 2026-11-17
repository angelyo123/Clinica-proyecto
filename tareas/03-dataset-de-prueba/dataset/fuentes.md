# Fuentes del conjunto de prueba

Fotos y escaneos de historias clínicas **llenadas a mano**, encontrados con Bing Imágenes el 2026-11-16. Son la imagen de portada (primera página) que publican Scribd, Studocu y SlideShare.

- Las imágenes **no se suben a git** (`.gitignore`): son de terceros y varias muestran datos personales de pacientes reales. Se quedan solo en `imagenes/` en esta máquina.
- Las transcripciones (`respuestas/`) tampoco se suben, por la misma razón.
- Si se pierden, se vuelven a descargar con estos enlaces. La resolución es la que dan esos sitios, menor que la de una foto de celular.

| Archivo | Documento | País | Resolución | Dificultad | Página de origen |
|---|---|---|---|---|---|
| hc-01.jpg | HC de hospitalización, formato impreso llenado a mano | Perú | 768×1024 | media | https://www.scribd.com/document/707352833/HISTORIA-CLI-NICA-DE-HOSPITALIZACIO-N-1 |
| hc-02.jpg | HC pediátrica, formato impreso llenado a mano | México | 768×1024 | fácil | https://www.scribd.com/document/867203957/Historia-Clinica-Para-Paciente |
| hc-03.jpg | HC odontológica, carátula de filiación llenada a mano | Ecuador | 768×1024 | fácil | https://www.scribd.com/document/986815652/Historias-Clinicas-Vinculacion-Escaneadas |
| hc-04.jpg | HC de semiología escrita a mano en hoja libre, foto con celular (datos personales difuminados) | Perú | 768×1024 | media | https://es.scribd.com/document/861946339/HISTORIA-CLI-NICA-COMPLETA-DE-SEMIOLOGIA |
| hc-05.jpg | Ficha clínica de admisión llenada a mano, con anotaciones de un docente | México | 768×1024 | fácil | https://www.scribd.com/document/747566670/Guia-Llenado-Ficha-Clinica-Julio-2023 |
| hc-06.jpg | Epicrisis psiquiátrica en cursiva, fotocopia con pliegues | Argentina | 768×1024 | difícil | https://es.scribd.com/document/707463236/Epicrisis-1 |
| hc-07.jpg | Epicrisis obstétrica con casillas (nombre tachado) | Argentina | 768×1024 | media | https://www.scribd.com/document/893718698/Epicrisis-Para-TP-jpg |
| hc-08.jpg | Notas de evolución y prescripciones, con anotaciones de un docente en rojo | Ecuador | 468×640 | extrema | https://es.slideshare.net/slideshow/texto-1-21709144/21709144 |
| hc-09.jpg | Notas de evolución y órdenes médicas | Honduras | 1200×1553 | difícil | https://www.studocu.com/latam/document/universidad-nacional-autonoma-de-honduras/enfermeria-y-sociedad-i/notas-de-evolucion-medica-katia-amador-hospital-san-jorge-el-hato/138145832 |

## Cómo se eligieron

Se revisaron unas 600 imágenes en hojas de miniaturas. Casi todo lo que hay publicado son formatos en blanco o llenados en computadora. Se descartaron:

- los formatos en blanco o llenados a máquina, aunque la fuente imite la letra a mano;
- los escaneos ilegibles (negros por la fotocopia);
- las notas sueltas que no son parte de una HC;
- las HC veterinarias.

## Cómo se transcribieron las respuestas

- Cada foto tiene su archivo `respuestas/hc-NN.json` con los campos de `esquema.json` que su formato tiene. Los que no aparecen deben quedar en `null`: si la IA los llena, cuentan como **inventados**.
- **`dudoso: true`**: el campo queda fuera de la medición. Se usa cuando ni la transcripción humana es segura, o cuando el campo es ambiguo (por ejemplo, varias fechas posibles).
- **Textos largos** (relato, antecedentes, diagnóstico, tratamiento): en vez del texto completo se anotan **claves**, frases cortas que se leen con seguridad. El campo se mide por la proporción de claves que aparecen en la respuesta de la IA.
- La transcripción la hizo Claude ampliando cada imagen. Hay que revisarla: los campos en duda están marcados.
