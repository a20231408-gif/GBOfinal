"""Apply identical test-only helper changes to both variants. No network."""
from pathlib import Path
P=Path(__file__).parent/'paquete'
source=(P/'BASE_V20_PRUEBAS/TestIsolation.gs').read_text('utf-8')
source=source.replace("const GBO_TEST_AUTHORIZED_ROOT", "const GBO_TEST_HARNESS_REVISION = 2;\nconst GBO_TEST_GUARD_POLICY = 'FRESH_WRITES_LOCKED_READ_CACHE_V2';\nvar gboTestGuard_={held:false,readOnly:false,reads:Object.create(null)};\nfunction gboTestInvalidateReads_(){gboTestGuard_.reads=Object.create(null);}\nfunction gboTestWrite_(ids,operation){\n  if(gboTestGuard_.readOnly)gboTestFail_('TEST_READ_ONLY');\n  gboTestInvalidateReads_();\n  ids.forEach(function(id){gboTestAssertId_(id,true);});\n  try{return operation();}finally{gboTestInvalidateReads_();}\n}\nconst GBO_TEST_AUTHORIZED_ROOT",1)
source=source.replace("start:function(action){this.state=", "start:function(action){gboTestGuard_.held=false;gboTestGuard_.readOnly=false;gboTestInvalidateReads_();this.state=",1)
source=source.replace('function gboTestAssertId_(id){','function gboTestAssertId_(id,forWrite){')
source=source.replace('var run=gboTestContextCheck_(), seen={}, original=id;', "var run=gboTestContextCheck_(), seen={}, original=id;\n  var cachedRead=!forWrite&&(gboTestGuard_.held||gboTestGuard_.readOnly);\n  if(cachedRead&&gboTestGuard_.reads[id]){GboTestMeasure.operation('guard.readCache.hit',function(){});return original;}")
source=source.replace("if(id===run.folderId)return original;", "if(id===run.folderId){\n      if((m.parents||[]).length!==1||m.parents[0]!==GBO_TEST_AUTHORIZED_ROOT)gboTestFail_('TEST_RUN_MOVED');\n      if(cachedRead)Object.keys(seen).forEach(function(k){gboTestGuard_.reads[k]=true;});\n      return original;\n    }")
# getId is an opaque local handle, not a read of remote data. Every operation
# using it still checks scope; no mutator trusts the read cache.
source=source.replace("var out={_gboNative:obj};", "var out={_gboNative:obj,getId:function(){return obj.getId();}};")
source=source.replace("['getId','getName','getDescription','getBlob']", "['getName','getDescription','getBlob']")
source=source.replace("gboTestAssertId_(obj.getId());GboTestMeasure.operation('backend.'+type+'.'+n,function(){return obj[n](value);});", "gboTestWrite_([obj.getId()],function(){return GboTestMeasure.operation('backend.'+type+'.'+n,function(){return obj[n](value);});});")
source=source.replace("gboTestAssertId_(obj.getId());return gboTestObject_(GboTestMeasure.operation('backend.Folder.createFolder',function(){return obj.createFolder(name);}), 'Folder');", "return gboTestWrite_([obj.getId()],function(){return gboTestObject_(GboTestMeasure.operation('backend.Folder.createFolder',function(){return obj.createFolder(name);}), 'Folder');});")
source=source.replace("gboTestAssertId_(obj.getId());return gboTestObject_(GboTestMeasure.operation('backend.Folder.createFile',function(){return obj.createFile(blob);}), 'File');", "return gboTestWrite_([obj.getId()],function(){return gboTestObject_(GboTestMeasure.operation('backend.Folder.createFile',function(){return obj.createFile(blob);}), 'File');});")
source=source.replace("gboTestAssertId_(obj.getId());gboTestAssertId_(parent.getId());GboTestMeasure.operation('backend.'+type+'.moveTo',function(){return obj.moveTo(gboTestNative_(parent));});", "gboTestWrite_([obj.getId(),parent.getId()],function(){return GboTestMeasure.operation('backend.'+type+'.moveTo',function(){return obj.moveTo(gboTestNative_(parent));});});")
source=source.replace("gboTestAssertId_(obj.getId());gboTestAssertId_(parent.getId());return gboTestObject_(GboTestMeasure.operation('backend.File.makeCopy',function(){return obj.makeCopy(name,gboTestNative_(parent));}),'File');", "return gboTestWrite_([obj.getId(),parent.getId()],function(){return gboTestObject_(GboTestMeasure.operation('backend.File.makeCopy',function(){return obj.makeCopy(name,gboTestNative_(parent));}),'File');});")
source=source.replace("return GboTestMeasure.operation('backend.Drive.Files.update',function(){return Drive.Files.update(meta,id,blob,options);});", "return gboTestWrite_([id],function(){return GboTestMeasure.operation('backend.Drive.Files.update',function(){return Drive.Files.update(meta,id,blob,options);});});")
source=source.replace("return GboTestMeasure.operation('backend.Drive.Files.create',function(){return Drive.Files.create(meta,blob,options);});", "return gboTestWrite_([meta.parents[0]],function(){return GboTestMeasure.operation('backend.Drive.Files.create',function(){return Drive.Files.create(meta,blob,options);});});")
source=source.replace("var a=GboTestMeasure.operation('backend.Drive.Files.generateIds'", "if(gboTestGuard_.readOnly)gboTestFail_('TEST_READ_ONLY');\n    var a=GboTestMeasure.operation('backend.Drive.Files.generateIds'")
source=source.replace("if(ok)row.at=Date.now();", "if(ok){row.at=Date.now();gboTestGuard_.held=true;gboTestInvalidateReads_();}")
source=source.replace("finally{if(row&&row.at!=null)", "finally{gboTestGuard_.held=false;gboTestInvalidateReads_();if(row&&row.at!=null)")
source=source.replace("gboTestAssertId_(m[1]);\n  return GboTestMeasure.operation('backend.video.openSession',function(){return UrlFetchApp.fetch(url,options);});", "return gboTestWrite_([m[1]],function(){return GboTestMeasure.operation('backend.video.openSession',function(){return UrlFetchApp.fetch(url,options);});});")
# Both facade properties and the active token remain test-only; read-only
# diagnostics do not use this mutation API or update the inflight counter.
source=source.replace("setProperty:function(key,value){var r=", "setProperty:function(key,value){if(gboTestGuard_.readOnly)gboTestFail_('TEST_READ_ONLY');var r=")
source=source.replace("deleteProperty:function(key){var r=", "deleteProperty:function(key){if(gboTestGuard_.readOnly)gboTestFail_('TEST_READ_ONLY');var r=")
for d in ['BASE_V20_PRUEBAS','CANDIDATO_OPTIMIZADO_PRUEBAS']:
    (P/d/'TestIsolation.gs').write_text(source,encoding='utf-8')
print('Common isolation v2 copied identically; full fresh ancestry remains mandatory for every Drive mutation.')
