const base = document.querySelector('meta[name="context-path"]').content;
const $ = (id) => document.getElementById(id);
const state = { dashboard: null, vehicles: [], records: [], slots: [], editingId: null, busy: false };
$('today').textContent = new Intl.DateTimeFormat('en', { weekday:'long', month:'long', day:'numeric', year:'numeric' }).format(new Date()).toUpperCase();
let toastTimer;
function toast(message, bad=false) { const node=$('toast'); node.textContent=message; node.classList.toggle('bad',bad); node.classList.add('show'); clearTimeout(toastTimer); toastTimer=setTimeout(()=>node.classList.remove('show'),3200); }
function esc(value) { return String(value ?? '').replace(/[&<>"']/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
function when(value) { if(!value)return '—'; const date=new Date(value); return Number.isNaN(date.valueOf())?esc(value):date.toLocaleString([], {month:'short',day:'numeric',hour:'numeric',minute:'2-digit'}); }
async function api(path, options={}) { const response=await fetch(base+'/api'+path,options); let body={}; try{body=await response.json()}catch{} if(!response.ok)throw new Error(body.error||`Request failed (${response.status})`); return body; }
function formOptions(values) { return new URLSearchParams(values).toString(); }
function setDb(ok, message) { $('dbStatus').textContent=message; $('dbDot').style.background=ok?'#67a472':'#d37568'; $('dbStatus').parentElement.classList.toggle('offline',!ok); $('sidebarDb').textContent=ok?'MySQL connected · auto refresh every 15 seconds':'Database connection failed'; }
function renderDashboard(data) {
  state.dashboard=data;
  $('totalSlots').textContent=data.totalSlots;
  $('availableSlots').textContent=data.availableSlots;
  $('parkedCount').textContent=data.parkedCount;
  $('vehicleCount').textContent=data.registeredVehicles;
  $('activeBadge').textContent=data.parkedCount;
  $('summaryRows').innerHTML=data.summary.length?data.summary.map(row=>{
    const percent=row.total?Math.round(row.occupied*100/row.total):0;
    return `<tr><td><span class="category ${row.category==='FACULTY'?'faculty':''}">${esc(title(row.category))}</span></td><td>${esc(typeName(row.vehicleType))}</td><td>${row.total}</td><td>${row.occupied}</td><td><b>${row.available}</b></td><td><div class="bar-cell"><span>${percent}%</span><span class="mini-bar"><i style="width:${percent}%"></i></span></div></td></tr>`
  }).join(''):'<tr><td colspan="6" class="empty">No parking slots in the database.</td></tr>';
  $('activeList').innerHTML=data.parkedVehicles.length?data.parkedVehicles.map(v=>`<div class="active-item"><span class="active-main">${esc(v.vehicleNumber)} · ${esc(v.ownerName)}</span><span class="active-detail">${esc(title(v.ownerCategory))} · ${esc(typeName(v.vehicleType))}</span><span class="slot-chip">${esc(v.slotNumber||'Slot assigned')}</span></div>`).join(''):'<div class="empty">No vehicles are parked right now.</div>';
  $('errorBanner').hidden=true;
}
function title(value) { return String(value||'').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase()); }
function typeName(value) { return value==='2-WHEELER'?'2-wheeler':value==='4-WHEELER'?'4-wheeler':value||'—'; }
function renderVehicles(rows) {
 state.vehicles=rows;
 $('vehicleTotal').textContent=`${rows.length} vehicle${rows.length===1?'':'s'}`;
 $('vehicleRows').innerHTML=rows.length?rows.map(v=>`<tr><td><b>${esc(v.vehicleNumber)}</b></td><td>${esc(v.ownerName)}</td><td><span class="category ${v.ownerCategory==='FACULTY'?'faculty':''}">${esc(title(v.ownerCategory))}</span></td><td>${esc(typeName(v.vehicleType))}</td><td><span class="status ${v.parked?'parked':'free'}">${v.parked?'Parked · '+esc(v.slotNumber||''): 'Not parked'}</span></td><td><div class="action-group"><button class="small-action" data-action="${v.parked?'exit':'park'}" data-id="${v.vehicleId}">${v.parked?'Release':'Park'}</button><button class="small-action" data-action="edit" data-id="${v.vehicleId}">Edit</button><button class="small-action text-danger" data-action="delete" data-id="${v.vehicleId}">Delete</button></div></td></tr>`).join(''):'<tr><td colspan="6" class="empty">No matching vehicles.</td></tr>';
}
function renderRecords(rows) {
 state.records=rows;
 $('recordRows').innerHTML=rows.length?rows.map(r=>`<tr><td><b>${esc(r.vehicleNumber)}</b></td><td>${esc(r.slotNumber)}</td><td>${when(r.entryTime)}</td><td>${when(r.exitTime)}</td><td><span class="status ${r.active?'parked':'free'}">${r.active?'Active':'Completed'}</span></td></tr>`).join(''):'<tr><td colspan="5" class="empty">No parking records found.</td></tr>';
}
function renderSlots(rows) {
 state.slots=rows;
 const query=$('slotSearch').value.trim().toLowerCase(), filter=$('slotFilter').value;
 const filtered=rows.filter(s=>(filter==='all'||(filter==='occupied')===s.occupied)&&(!query||s.slotNumber.toLowerCase().includes(query)));
 $('slotRows').innerHTML=filtered.length?filtered.map(s=>`<tr><td><b>${esc(s.slotNumber)}</b></td><td>${esc(title(s.category))}</td><td>${esc(typeName(s.slotType))}</td><td><span class="status ${s.occupied?'parked':'free'}">${s.occupied?'Occupied':'Available'}</span></td></tr>`).join(''):'<tr><td colspan="4" class="empty">No slots match this filter.</td></tr>';
 $('slotTotal').textContent=`${filtered.length} of ${rows.length} slots`;
}
async function refresh(showMessage=false) {
 if(state.busy)return; state.busy=true;
 try {
  const [health,dash,vehicles,records,slots]=await Promise.all([api('/health'),api('/dashboard'),api('/vehicles'),api('/records'),api('/slots')]);
  renderDashboard(dash); renderVehicles(vehicles); renderRecords(records); renderSlots(slots); setDb(true,'MySQL connected');
  if(showMessage)toast('Parking data refreshed from MySQL.');
 } catch(error) {
  setDb(false,'MySQL unavailable'); $('errorBanner').textContent=error.message; $('errorBanner').hidden=false;
  if(showMessage)toast(error.message,true);
 } finally { state.busy=false; }
}
function openDialog(vehicle=null) {
 const form=$('vehicleForm'); form.reset(); state.editingId=vehicle?vehicle.vehicleId:null;
 $('dialogTitle').textContent=vehicle?'Update vehicle':'Register a vehicle'; $('saveVehicle').textContent=vehicle?'Save changes':'Save vehicle';
 if(vehicle){form.elements.vehicleNumber.value=vehicle.vehicleNumber;form.elements.ownerName.value=vehicle.ownerName;form.elements.ownerCategory.value=vehicle.ownerCategory;form.elements.vehicleType.value=vehicle.vehicleType;}
 $('vehicleDialog').showModal();
}
function closeDialog(){ $('vehicleDialog').close(); state.editingId=null; }
$('addVehicleBtn').addEventListener('click',()=>openDialog()); $('vehicleAddBtn').addEventListener('click',()=>openDialog()); $('closeDialog').addEventListener('click',closeDialog); $('cancelDialog').addEventListener('click',closeDialog); $('refreshBtn').addEventListener('click',()=>refresh(true));
$('vehicleForm').addEventListener('submit',async e=>{e.preventDefault();const values=Object.fromEntries(new FormData(e.currentTarget));const editing=state.editingId;const path=editing?`/vehicles/${editing}`:'/vehicles';try{await api(path,{method:editing?'PUT':'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:formOptions(values)});closeDialog();toast(editing?'Vehicle updated.':'Vehicle registered.');await refresh();}catch(error){toast(error.message,true);}});
$('vehicleRows').addEventListener('click',async e=>{const button=e.target.closest('button[data-action]');if(!button)return;const id=Number(button.dataset.id),action=button.dataset.action,vehicle=state.vehicles.find(v=>v.vehicleId===id);try{
 if(action==='edit')return openDialog(vehicle);
 if(action==='delete'){if(!confirm(`Delete ${vehicle?.vehicleNumber||'this vehicle'}? This removes its registration from MySQL.`))return;await api(`/vehicles/${id}`,{method:'DELETE'});toast('Vehicle deleted.');}
 if(action==='park'){const result=await api(`/parking/${id}/park`,{method:'POST'});toast(`Parked in ${result.slotNumber}.`);}
 if(action==='exit'){const result=await api(`/parking/${id}/exit`,{method:'POST'});toast(`Released from ${result.slotNumber}.`);}
 await refresh();
 }catch(error){toast(error.message,true);}});
let vehicleTimer,recordTimer;
$('vehicleSearch').addEventListener('input',()=>{clearTimeout(vehicleTimer);vehicleTimer=setTimeout(async()=>{try{renderVehicles(await api('/vehicles?q='+encodeURIComponent($('vehicleSearch').value.trim())))}catch(error){toast(error.message,true)}},250)});
$('clearVehicleSearch').addEventListener('click',()=>{$('vehicleSearch').value='';refresh()});
$('recordSearch').addEventListener('input',()=>{clearTimeout(recordTimer);recordTimer=setTimeout(async()=>{try{renderRecords(await api('/records?q='+encodeURIComponent($('recordSearch').value.trim())))}catch(error){toast(error.message,true)}},250)});
$('slotSearch').addEventListener('input',()=>renderSlots(state.slots)); $('slotFilter').addEventListener('change',()=>renderSlots(state.slots));
refresh(); setInterval(()=>refresh(),15000);
