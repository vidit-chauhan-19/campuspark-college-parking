(() => {
  const base = document.querySelector('meta[name="context-path"]')?.content || '';
  const style = document.createElement('style');
  style.textContent = `#cp-auth-shade{position:fixed;inset:0;z-index:9999;background:#f5f7f2;display:grid;place-items:center;padding:24px;font-family:Inter,system-ui,sans-serif}#cp-auth-shade[hidden]{display:none}.cp-auth-card{width:min(420px,100%);background:#fff;border:1px solid #e5e9e2;border-radius:22px;padding:34px;box-shadow:0 24px 80px #182b201c}.cp-auth-mark{font-size:14px;font-weight:800;letter-spacing:.03em;color:#17231c}.cp-auth-mark b{color:#49834e}.cp-auth-card .cp-eyebrow{margin:28px 0 8px;color:#64856a;font-size:11px;font-weight:800;letter-spacing:.13em}.cp-auth-card h1{margin:0;font-size:29px;color:#17231c}.cp-auth-card p{color:#748078;font-size:14px;line-height:1.6}.cp-auth-card label{display:block;margin-top:16px;font-size:12px;font-weight:700;color:#34443a}.cp-auth-card input{box-sizing:border-box;width:100%;margin-top:7px;padding:12px;border:1px solid #dce3db;border-radius:10px;font:inherit}.cp-auth-card button{width:100%;margin-top:22px;background:#376f46;border:0;border-radius:10px;color:white;padding:13px;font-weight:750;cursor:pointer}.cp-auth-error{color:#b53c34;min-height:1.2em;font-size:13px}.cp-account{display:flex;align-items:center;gap:10px;margin-left:12px}.cp-account small{color:#657267}.cp-account button{border:1px solid #dce3db;background:#fff;border-radius:8px;padding:7px 10px;cursor:pointer}.viewer-mode #addVehicleBtn,.viewer-mode #vehicleAddBtn,.viewer-mode #vehicleRows td:last-child,.viewer-mode #vehicleActionsHeader{display:none!important}body.cp-locked{overflow:hidden}`;
  document.head.append(style);
  document.body.classList.add('cp-locked');
  const shade = document.createElement('section'); shade.id='cp-auth-shade'; shade.setAttribute('aria-label','Sign in');
  shade.innerHTML = '<form class="cp-auth-card" id="cp-auth-form"><div class="cp-auth-mark">CAMPUS<b>PARK</b> · COLLEGE PARKING</div><div class="cp-eyebrow">SECURE PARKING PORTAL</div><h1>Welcome back</h1><p>Sign in with your campus account to view parking operations.</p><label>Username<input name="username" autocomplete="username" required></label><label>Password<input name="password" type="password" autocomplete="current-password" required></label><div class="cp-auth-error" id="cp-auth-error" role="alert"></div><button type="submit">Sign in</button><p>Admins can manage parking. Viewer accounts are read-only.</p></form>';
  document.body.append(shade);
  const setAccount = account => {
    if (!account?.authenticated) return;
    shade.hidden = true; document.body.classList.remove('cp-locked');
    document.body.classList.toggle('viewer-mode', account.role === 'VIEWER');
    let area=document.querySelector('.cp-account');
    if(!area){area=document.createElement('div');area.className='cp-account';document.querySelector('header .head-right')?.append(area)}
    area.innerHTML='<small></small><button type="button">Sign out</button>';
    area.querySelector('small').textContent=account.username+' · '+account.role.toLowerCase();
    area.querySelector('button').onclick=async()=>{await fetch(base+'/api/auth/logout',{method:'POST',credentials:'same-origin'});location.reload()};
    document.querySelector('.profile b')?.replaceChildren(document.createTextNode(account.username));
    document.querySelector('.profile small')?.replaceChildren(document.createTextNode(account.role==='ADMIN'?'Administrator':'Read-only viewer'));
  };
  shade.querySelector('form').addEventListener('submit',async event=>{
    event.preventDefault();const form=event.currentTarget,button=form.querySelector('button');button.disabled=true;
    const error=shade.querySelector('#cp-auth-error');error.textContent='';
    try{const response=await fetch(base+'/api/auth/login',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams(new FormData(form))});const body=await response.json();if(!response.ok)throw new Error(body.error||'Sign-in failed');location.reload()}
    catch(err){error.textContent=err.message||'Sign-in unavailable';button.disabled=false}
  });
  fetch(base+'/api/auth/session',{credentials:'same-origin',cache:'no-store'}).then(r=>r.json()).then(setAccount).catch(()=>{});
  const oldFetch=window.fetch.bind(window);window.fetch=(...args)=>oldFetch(...args).then(response=>{if(response.status===401&&String(args[0]).includes('/api/')&&!String(args[0]).includes('/api/auth/')){shade.hidden=false;document.body.classList.add('cp-locked')}return response});
})();
