"""Update only assertions contradicted by the user's explicit new SCAP contract."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'app/src/test/java/com/tuempresa/inventariovial'
p=root/'ParallelBScapCaptureTest.kt';s=p.read_text(encoding='utf-8')
s=s.replace('''val code=(cells["A$row"] as Number).toInt().toString();val element=cat.element(code)!!
                assertTrue(code in cat.defaultElements)''','''val code=(cells["A$row"] as Number).toInt().toString()
                val canonical=cat.canonicalElements.single{it.exportCode==code}
                val element=cat.element(canonical.templateCode)!!
                assertTrue(canonical.templateCode in cat.defaultElements)''')
a=s.index('    @Test fun manualGPreserves');b=s.index('    @Test fun canonicalManualCoordinates',a)
part=s[a:b].replace('elements.single()','elements.single{it.element.elementCode=="X-42"}')
part=part.replace('assertTrue(review.errors.any{it.contains("evaluación")})','assertTrue(review.pending.any{it.contains("evaluación") || it.contains("incompletos")})')
part=part.replace('it.section=="G1" && it.field.contains("X-42")','it.section=="G1" && it.ownerId==element.id')
s=s[:a]+part+s[b:]
a=s.index('    @Test fun finalizationValidates');b=s.index('    @Test fun observedNumeric',a)
part=s[a:b].replace('elements.single()','elements.single{it.element.elementCode=="104"}')
part=part.replace('''            assertTrue(runCatching{repo.complete(id)}.isFailure)
            repo.setField''','''            repo.complete(id)
            assertEquals("INCOMPLETE",repo.dao.snapshot(id)!!.values("completion")["status"])
            repo.reopen(id)
            repo.setField''')
part=part.replace('assertEquals(1,repo.dao.snapshot(id)!!.elements.size)','assertEquals(17,repo.dao.snapshot(id)!!.elements.size)')
s=s[:a]+part+s[b:]
p.write_text(s,encoding='utf-8')
p=root/'ScapCanonicalExportTest.kt';s=p.read_text(encoding='utf-8')
s=s.replace('''        assertTrue(ScapDelivery.exporter(context,overflow).review(overflow).errors.any {"DETALLES" in it})''','''        assertTrue(ScapDelivery.exporter(context,overflow).review(overflow).errors.isEmpty())
        val extended=export(overflow)
        assertTrue(extended.sheet(3).nodes("c").any{it.textContent=="X3"})
        val tooMany=overflow.copy(elements=(1..32).map{i->overflow.elements.first().let{it.copy(element=it.element.copy(id="over$i",elementCode="OVER$i"))}})
        assertTrue(ScapDelivery.exporter(context,tooMany).review(tooMany).errors.any{"31" in it})''')
p.write_text(s,encoding='utf-8')
p=root/'ScapStorageTest.kt';s=p.read_text(encoding='utf-8')
s=s.replace('''            for(value in listOf("", "99", "101", "-1", "NaN")) {''','''            for(value in listOf("", "99")) {
                r.setField(id,e.id,"percent1",value)
                r.complete(id)
                assertEquals("INCOMPLETE",r.dao.snapshot(id)!!.values("completion")["status"])
                r.reopen(id)
            }
            for(value in listOf("101", "-1", "NaN")) {''')
s=s.replace('''            assertTrue(runCatching{r.complete(id)}.isFailure) // A selected, unevaluated element blocks closure.
            r.selectElement''','''            assertTrue(ScapValidation.review(r.dao.snapshot(id)!!,r.catalog).pending.any{it.contains("110")})
            r.complete(id);assertEquals("INCOMPLETE",r.dao.snapshot(id)!!.values("completion")["status"]);r.reopen(id)
            r.selectElement''')
s=s.replace('assertTrue(ScapValidation.review(changed,r.catalog).errors.any{it.contains("categoría y tipo")})','assertTrue(ScapValidation.review(changed,r.catalog).pending.any{it.contains("categoría y tipo")})')
p.write_text(s,encoding='utf-8')
