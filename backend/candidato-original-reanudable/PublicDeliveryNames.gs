/* Negotiated display labels only. Bindings and prior photo reservations remain immutable. */
var PublicDeliveryNames=(()=>{
 const categories={"INICIO": "INICIO", "FIN": "FIN", "OBSERVACION": "OBSERVACION", "PANORAMICA": "PANORAMICA", "ELEMENTO": "ELEMENTO", "CULVERT_PANORAMIC": "PANORAMICA", "CULVERT_INLET": "ENTRADA", "CULVERT_OUTLET": "SALIDA", "RIGHT_SIDE": "VISTA_LADO_DERECHO", "LEFT_SIDE": "VISTA_LADO_IZQUIERDO", "UPSTREAM": "AGUAS_ARRIBA", "DOWNSTREAM": "AGUAS_ABAJO", "ROAD_DECK": "CARRETERA_TABLERO", "INFORMATIVE_SIGN": "SENAL_INFORMATIVA", "RAILINGS_SIDEWALKS_PIPES": "BARANDAS_VEREDAS_TUBERIAS", "DEFECT": "ANOMALIAS_FALLAS", "BRIDGE_ELEMENT": "OTROS_ELEMENTOS_PUENTE", "TRANSVERSE": "VISTA_TRANSVERSAL", "ELEMENT": "ELEMENTO", "ACCESS": "ACCESO", "CHANNEL": "CAUCE", "OTHER": "OTRA"};
 function component(value,bytes=64){
  let text=String(value).normalize('NFC').replace(/[\p{Cc}\p{Cf}<>:"/\\|?*]/gu,' ').split('..').join(' ').replace(/\s+/g,' ').replace(/^[ .-]+|[ .-]+$/g,'')||'Sin referencia';
  let result='',used=0;
  for(const ch of text){const cp=ch.codePointAt(0),n=cp<128?1:cp<2048?2:cp<65536?3:4;if(used+n>bytes)break;result+=ch;used+=n;}
  result=result.replace(/[ .-]+$/g,'')||'Ficha';
  if(/^(CON|PRN|AUX|NUL|COM[0-9]+|LPT[0-9]+)$/.test(result.split('.')[0].toUpperCase()))return component('Ficha - '+result,bytes);
  return result;
 }
 function photoName(label,category,number){
  if(!Number.isSafeInteger(number)||number<1||number>999)SyncV3.reject('READABLE_NAME_LIMIT_OR_CATALOG');
  const panel=typeof ScapPhotoPanelV2!=='undefined'?ScapPhotoPanelV2.get(category):null;
  return component(label,42)+' - '+component((panel?.folder||categories[category]||category).replace(/_/g,' '),16)+' - Foto '+number+'.jpg';
 }
 return {component,photoName};
})();
if(typeof module!=='undefined')module.exports=PublicDeliveryNames;
