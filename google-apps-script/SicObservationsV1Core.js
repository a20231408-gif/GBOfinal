/* Only the optional trailing OBSERVACIONES column is reconciled. No Drive calls. */
var SicObservationsV1=(()=>{
  function reject(){const e=Error('OBSERVATIONS_TEMPLATE_CONFLICT');e.code='OBSERVATIONS_TEMPLATE_CONFLICT';throw e;}
  const cells=row=>row.match(/<c\b[^>]*(?:\/>|>[\s\S]*?<\/c>)/g) || [];
  const col=cell=>(/\br="([A-Z]+)\d+"/.exec(cell)||[])[1];
  const number=c=>[...c].reduce((n,x)=>n*26+x.charCodeAt(0)-64,0);
  const letters=n=>n<=26?String.fromCharCode(64+n):letters(Math.floor((n-1)/26))+String.fromCharCode(65+(n-1)%26);
  function plain(sheet,c) {
    if(!c)return sheet;
    // Keep all contractual cells/styles untouched; only the trailing note uses Normal.
    sheet=sheet.replace(/<c\b[^>]*(?:\/>|>[\s\S]*?<\/c>)/g,cell=>col(cell)===c?cell.replace(/\s+s="[^"]*"/,''):cell);
    sheet=sheet.replace(/<col\b[^>]*\/>/g,def=>Number(/\bmin="(\d+)"/.exec(def)?.[1])===number(c) && Number(/\bmax="(\d+)"/.exec(def)?.[1])===number(c)?'':def);
    return sheet.replace(/<autoFilter\b[^>]*(?:\/>|>[\s\S]*?<\/autoFilter>)/g,filter=> {
      const m=/\bref="([A-Z]+)(\d+)(?::([A-Z]+)(\d+))?"/.exec(filter);
      if(!m)return filter;
      const first=number(m[1]),last=number(m[3] || m[1]),n=number(c);
      if(first>n || last<n)return filter;
      if(first===n)return '';
      return filter.replace(m[0],`ref="${m[1]}${m[2]}:${letters(n-1)}${m[4] || m[2]}"`)
        .replace(/<filterColumn\b[^>]*(?:\/>|>[\s\S]*?<\/filterColumn>)/g,x=>Number(/\bcolId="(\d+)"/.exec(x)?.[1])>=n-first?'':x);
    });
  }
  function inspect(sheet,start=14) {
    const header=(sheet.match(/<row\b[^>]*\br="13"[^>]*>[\s\S]*?<\/row>/)||[])[0] || '';
    const columns=(/<cols>([\s\S]*?)<\/cols>/.exec(sheet)||[])[1] || '';
    if(start!==14)return {header,columns,observationColumn:null};
    const all=cells(header),found=all.filter(c=>/<t\b[^>]*>\s*OBSERVACIONES\s*<\/t>/.test(c));
    if(found.length>1)reject();
    if(!found.length)return {header,columns,observationColumn:null};
    const c=col(found[0]);if(!c || all.some(x=>number(col(x))>number(c)))reject();
    if(number(c)!==Math.max(0,...all.filter(x=>x!==found[0]).map(x=>number(col(x))))+1)reject();
    const definitions=columns.match(/<col\b[^>]*\/>/g) || [];
    let baseColumns=columns;
    for(const def of definitions) {
      const lo=Number(/\bmin="(\d+)"/.exec(def)?.[1]),hi=Number(/\bmax="(\d+)"/.exec(def)?.[1]);
      if(lo<=number(c) && hi>=number(c)) {if(lo!==hi)reject();baseColumns=baseColumns.replace(def,'');}
    }
    return {header:header.replace(found[0],''),columns:baseColumns,observationColumn:c};
  }
  function inherit(current,previous) {
    if(current.observationColumn || !previous?.observationColumn)return current;
    if(current.signature!==previous.signature)reject();
    const c=previous.observationColumn;
    const old=cells(previous.rows[0]).find(x=>col(x)===c);
    // Previous metadata supplies layout only; every original data cell is from
    // the newly accepted UUID row. A legacy client cannot erase a known note.
    return {...previous,rows:current.rows.map(row=>row.replace('</row>',(old || '')+'</row>'))};
  }
  function select(books) {
    if(!books.length || books.some(b=>b.signature!==books[0].signature))reject();
    const book=books.find(b=>b.observationColumn) || books[0];
    if(!book.observationColumn)return book;
    const c=book.observationColumn,n=number(c);
    let sheet=book.sheet;
    sheet=sheet.replace(/(<dimension\b[^>]*\bref="[A-Z]+\d+:)([A-Z]+)(\d+")/g,
      (_,a,b,d)=>a+(number(b)<n?c:b)+d);
    return {...book,sheet:plain(sheet,c)};
  }
  function printExtent(xml,c) {if(!c)return xml;return xml.replace(/(!\$A\$\d+:\$)([A-Z]+)(\$\d+)/g,(_,a,b,d)=>a+(number(b)<number(c)?c:b)+d);}
  return {inspect,inherit,select,printExtent,plain};
})();
if(typeof module!=='undefined')module.exports=SicObservationsV1;
