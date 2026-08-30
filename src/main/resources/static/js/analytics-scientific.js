const loadingState = document.querySelector("#loading-state");
const analyticsContent = document.querySelector("#analytics-content");
const pageMessage = document.querySelector("#page-message");
let selectedOrganizationId;
let selectedHistoryPeriod = "LAST_24_HOURS";
let currentAuth;
let refreshInProgress = false;
let selectedSessionId = null;
let sessionCharts = [];

const request = url => apiRequest(url);

async function initialize(organization) {
    if (!organization) { loadingState.textContent = "No organization access is available."; return; }
    try {
        currentAuth = await labMonitorAuthReady;
        configureRoleCopy(organization.id);
        await loadOverview(organization.id);
    } catch (error) {
        loadingState.classList.add("hidden"); showMessage(error.message);
    }
}

async function loadOverview(organizationId, {silent = false} = {}) {
    selectedOrganizationId = organizationId;
    if (!silent) { loadingState.classList.remove("hidden"); analyticsContent.classList.add("hidden"); hideMessage(); }
    try {
        const base = `/api/analytics/organizations/${organizationId}`;
        const [overview, problemRooms, data, history] = await Promise.all([
            request(`${base}/overview`), request(`${base}/problem-rooms`), loadDashboardData(organizationId),
            request(`${base}/history?period=${selectedHistoryPeriod}`)
        ]);
        if (String(selectedOrganizationId) !== String(organizationId)) return;
        renderStatus(overview, data, problemRooms);
        renderOperationalState(overview, data, problemRooms);
        renderSessions(data.sessions, organizationId);
        renderRecentActivity(data.alerts, data);
        renderHistory(history);
        updateLinks(organizationId);
        document.querySelector("#updated-at").textContent = `Updated ${relativeTime(new Date(overview.generatedAt))}`;
        analyticsContent.classList.remove("hidden"); updateUrl(organizationId);
    } catch (error) { if (!silent) showMessage(error.message); }
    finally { if (!silent) loadingState.classList.add("hidden"); }
}

async function loadDashboardData(organizationId) {
    const [labs, rooms, sensors, alerts, sessions] = await Promise.all([
        request(`/api/labs?organizationId=${organizationId}`), request("/api/rooms"), request("/api/sensors"),
        request(`/api/alerts?organizationId=${organizationId}`), request("/api/monitoring-sessions")
    ]);
    return {labs, rooms: rooms.filter(item => String(item.organizationId) === String(organizationId)), sensors: sensors.filter(item => String(item.organizationId) === String(organizationId)), alerts,
        sessions: sessions.filter(item => String(item.organizationId) === String(organizationId))};
}

function configureRoleCopy(organizationId) {
    const role = currentAuth.user.globalRole === "SUPER_ADMIN" ? "SUPER_ADMIN" : currentAuth.membership(organizationId)?.role;
    const intro = role === "LAB_ADMIN" ? "Operational overview of your laboratory environment"
        : role === "LIMITED_EMPLOYEE" ? "Operational overview of your monitored environments"
            : "Operational overview of the selected organization";
    document.querySelector("#overview-intro").textContent = intro;
}

function renderStatus(overview, data, problemRoomDetails) {
    const grid = document.querySelector("#scientific-status-grid"); grid.replaceChildren();
    const openAlerts = data.alerts.filter(alert => alert.status !== "RESOLVED");
    const problemRooms = Number(overview.roomsRequiringAttention || 0), offlineSensors = Number(overview.offlineSensors || 0);
    const organization = selectedOrganizationId;
    const roomsHref = problemRooms === 1 && problemRoomDetails.length === 1
        ? `/alerts.html?organizationId=${organization}&roomId=${problemRoomDetails[0].roomId}&openOnly=true`
        : problemRooms ? `/alerts.html?organizationId=${organization}&openOnly=true` : `/monitor.html?organizationId=${organization}`;
    const items = [
        {label:"ROOMS",icon:"□",value:problemRooms || data.rooms.length,state:problemRooms?"need attention":data.rooms.length?"healthy":"none available",secondary:data.rooms.length,detail:"accessible",attention:problemRooms>0,href:roomsHref},
        {label:"SENSORS",icon:"⌁",value:offlineSensors || data.sensors.length,state:offlineSensors?"offline":data.sensors.length?"online":"none available",secondary:data.sensors.length,detail:"accessible",attention:offlineSensors>0,href:`/monitor.html?organizationId=${organization}${offlineSensors?"&sensorStatus=OFFLINE":""}`},
        {label:"ALERTS",icon:"△",value:openAlerts.length,state:openAlerts.length?"open":"no open alerts",secondary:Number(overview.criticalAlerts||0),detail:"critical",attention:openAlerts.length>0,href:`/alerts.html?organizationId=${organization}${openAlerts.length?"&openOnly=true":""}`}
    ];
    items.forEach(item => {
        const link = document.createElement("a"); link.className = `scientific-status-item${item.attention?" is-attention":""}`; link.href = item.href;
        link.setAttribute("aria-label", `${item.label}: ${item.value} ${item.state}`);
        const icon = span("scientific-status-icon", item.icon), copy = span("scientific-status-copy");
        copy.append(span("scientific-status-label",item.label), strong("scientific-status-value",item.value), span("scientific-status-state",item.state), span("scientific-status-divider"), strong("scientific-status-secondary",item.secondary), span("scientific-status-muted",item.detail));
        link.append(icon,copy); grid.append(link);
    });
}

function renderOperationalState(overview, data, rooms) {
    const attention = document.querySelector("#attention-section"), healthy = document.querySelector("#healthy-section"), empty = document.querySelector("#empty-section");
    [attention,healthy,empty].forEach(item => item.classList.add("hidden"));
    if (!data.rooms.length && !data.sensors.length) { empty.classList.remove("hidden"); return; }
    const openAlerts = data.alerts.some(alert => alert.status !== "RESOLVED");
    if (rooms.length || overview.offlineSensors > 0 || openAlerts) { attention.classList.remove("hidden"); renderProblems(rooms, overview.roomsRequiringAttention); }
    else healthy.classList.remove("hidden");
}

function renderProblems(rooms, total) {
    const list = document.querySelector("#problem-list"), more = document.querySelector("#more-problems-link"); list.replaceChildren();
    if (!rooms.length) list.append(emptyState("Monitoring issue detected", "Open alerts or offline sensors still require review."));
    rooms.slice(0,4).forEach(room => {
        const link = document.createElement("a"); link.className = "scientific-problem-row"; link.href = `/alerts.html?organizationId=${selectedOrganizationId}&roomId=${room.roomId}&openOnly=true`;
        const severity = span(`scientific-severity ${room.attentionLevel === "CRITICAL"?"critical":"warning"}`,room.attentionLevel);
        const location = span("scientific-problem-location"); location.append(strong("",room.roomName),small("",room.labName||""));
        const issue = span("scientific-problem-issue",room.mainProblem||"Monitoring issue");
        link.append(severity,location,issue,span("scientific-problem-age",formatDuration(room.openMinutes)),span("scientific-chevron","›")); list.append(link);
    });
    const hidden = Math.max(0,Number(total||rooms.length)-Math.min(4,rooms.length)); more.classList.toggle("hidden",!hidden); more.textContent = hidden?`+${hidden} more requiring attention`:""; more.href=`/alerts.html?organizationId=${selectedOrganizationId}&openOnly=true`;
}

function renderSessions(sessions, organizationId) {
    destroySessionCharts();
    const active = sessions.filter(item => item.status === "ACTIVE"), completed = sessions.filter(item => item.status === "COMPLETED" && item.startedAt);
    const choices = (active.length?active:completed).slice(0,4), isActive = active.length>0;
    const select = document.querySelector("#active-session-select"), state = document.querySelector("#active-session-state"), charts = document.querySelector("#active-session-charts"), dot = document.querySelector("#session-live-dot"), title = document.querySelector("#session-panel-title");
    select.replaceChildren(); charts.replaceChildren(); dot.classList.toggle("inactive",!isActive);
    title.textContent = isActive?"LIVE MONITORING":choices.length?"LATEST COMPLETED SESSION":"LIVE MONITORING";
    if (!choices.length) { select.classList.add("hidden"); state.classList.remove("hidden"); state.textContent="No session data yet. Start a monitoring session; its chart will appear after the first reading."; return; }
    select.classList.toggle("hidden",choices.length===1); choices.forEach(item=>select.append(new Option(`${item.name} · ${item.roomName}`,item.id)));
    const selected = choices.find(item=>String(item.id)===String(selectedSessionId))||choices[0]; selectedSessionId=selected.id; select.value=selected.id;
    select.onchange=()=>{selectedSessionId=Number(select.value); const session=choices.find(item=>item.id===selectedSessionId); if(session)loadSessionTimeline(session);};
    loadSessionTimeline(selected);
}

async function loadSessionTimeline(session) {
    const state=document.querySelector("#active-session-state"),container=document.querySelector("#active-session-charts"); destroySessionCharts(); container.replaceChildren(); state.classList.remove("hidden"); state.textContent="Loading session readings…";
    try { const timeline=await request(`/api/monitoring-sessions/${session.id}/timeline`); if(String(selectedSessionId)!==String(session.id))return;
        if(!timeline.readings.length){state.textContent="No sensor readings were recorded during this session.";return;} state.classList.add("hidden");
        const groups=new Map(); timeline.readings.forEach(reading=>{const unit=reading.unit||"Value";if(!groups.has(unit))groups.set(unit,[]);groups.get(unit).push(reading);});
        groups.forEach((readings,unit)=>{const section=document.createElement("section");section.className="scientific-session-chart";const heading=document.createElement("h3");heading.textContent=unit==="Value"?"Sensor values":unit;const holder=document.createElement("div");holder.className="scientific-session-canvas";const canvas=document.createElement("canvas");holder.append(canvas);section.append(heading,holder);container.append(section);const count=new Set(readings.map(item=>item.sensorId)).size;sessionCharts.push(LabMonitorSessionChart.create(canvas,timeline,readings,{showLegend:count>1}));});
    } catch(error){state.textContent=error.message;}
}
function destroySessionCharts(){sessionCharts.forEach(chart=>chart.destroy());sessionCharts=[];}

function renderRecentActivity(alerts,data){const list=document.querySelector("#recent-activity-list");list.replaceChildren();const recent=alerts.slice().sort((a,b)=>new Date(b.createdAt)-new Date(a.createdAt)).slice(0,5);if(!recent.length){list.append(emptyState("No recent alert activity","New monitoring events will appear here."));return;}recent.forEach(alert=>{const link=document.createElement("a");link.className="scientific-activity-row";link.href=`/alerts.html?organizationId=${selectedOrganizationId}&roomId=${alert.roomId}&alertId=${alert.id}`;const icon=span(`scientific-activity-icon ${severityTone(alert.severity)}`,alert.status==="RESOLVED"?"✓":"△"),copy=span("scientific-activity-copy");copy.append(strong("",alert.title||"Monitoring alert"),span("",`${alert.roomName||`Room ${alert.roomId}`} · ${alert.sensorName||"Room alert"}`));link.append(icon,copy,span("scientific-activity-time",relativeTime(new Date(alert.createdAt))));list.append(link);});}

async function loadHistory(period){selectedHistoryPeriod=period;const loading=document.querySelector("#history-loading-state"),content=document.querySelector("#trend-content");loading.classList.remove("hidden");content.classList.add("hidden");try{const history=await request(`/api/analytics/organizations/${selectedOrganizationId}/history?period=${period}`);renderHistory(history);content.classList.remove("hidden");}catch(error){showMessage(error.message);}finally{loading.classList.add("hidden");}}
function renderHistory(history){document.querySelector("#history-created").textContent=history.alertsCreated??0;document.querySelector("#average-response").textContent=formatOptionalDuration(history.averageAcknowledgementMinutes);document.querySelector("#average-resolution").textContent=formatOptionalDuration(history.averageResolutionMinutes);renderTrend(history.dailyAlerts||[]);}
function renderTrend(days){const root=document.querySelector("#history-chart");root.replaceChildren();if(!days.length){root.append(emptyState("No alert history","No alerts were created in this period."));return;}const width=720,height=210,pad=14,values=days.map(day=>Number(day.alerts||0)),max=Math.max(1,...values);const points=values.map((value,index)=>[pad+index*((width-pad*2)/Math.max(1,values.length-1)),height-pad-value/max*(height-pad*2)]);const path=smoothPath(points),area=`${path} L ${points.at(-1)[0]} ${height-pad} L ${points[0][0]} ${height-pad} Z`,labels=days.map(day=>formatChartDate(day.date||day.day));root.innerHTML=`<div class="scientific-chart-grid"><i></i><i></i><i></i><i></i></div><svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true"><defs><linearGradient id="scientificAreaFill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="#ef4b43" stop-opacity=".22"></stop><stop offset="100%" stop-color="#ef4b43" stop-opacity=".025"></stop></linearGradient></defs><path class="scientific-chart-area" d="${area}"></path><path class="scientific-chart-line" d="${path}"></path></svg><div class="scientific-chart-labels"><span>${labels[0]||""}</span><span>${labels[Math.floor((labels.length-1)/2)]||""}</span><span>${labels.at(-1)||""}</span></div>`;}
function smoothPath(points){if(points.length===1)return`M ${points[0][0]} ${points[0][1]}`;let d=`M ${points[0][0]} ${points[0][1]}`;for(let i=1;i<points.length;i++){const previous=points[i-1],current=points[i],mid=(previous[0]+current[0])/2;d+=` C ${mid} ${previous[1]}, ${mid} ${current[1]}, ${current[0]} ${current[1]}`;}return d;}

function updateLinks(id){document.querySelector("#sessions-link").href=`/monitoring-sessions.html?organizationId=${id}`;document.querySelector("#all-alerts-link").href=`/alerts.html?organizationId=${id}&openOnly=true`;document.querySelector("#healthy-monitor-link").href=`/monitor.html?organizationId=${id}`;document.querySelector("#history-link").href=`/history.html?organizationId=${id}`;}
function emptyState(title,detail){const item=document.createElement("div");item.className="scientific-inline-empty";item.append(strong("",title),span("",detail));return item;}
function span(className,text){const item=document.createElement("span");if(className)item.className=className;if(text!=null)item.textContent=text;return item;}function strong(className,text){const item=document.createElement("strong");if(className)item.className=className;item.textContent=text;return item;}function small(className,text){const item=document.createElement("small");if(className)item.className=className;item.textContent=text;return item;}
function severityTone(value){return ["CRITICAL","HIGH"].includes(value)?"danger":value==="MEDIUM"?"warning":"";}function formatOptionalDuration(value){return value==null?"—":formatDuration(value);}function formatDuration(minutes){const value=Math.max(0,Number(minutes||0));if(value<60)return`${Math.round(value)}m`;const hours=Math.floor(value/60),remainder=Math.round(value%60);if(hours<24)return remainder?`${hours}h ${remainder}m`:`${hours}h`;const days=Math.floor(hours/24),left=hours%24;return left?`${days}d ${left}h`:`${days}d`;}
function relativeTime(date){const seconds=Math.max(0,Math.round((Date.now()-date.getTime())/1000));if(seconds<60)return"just now";const minutes=Math.floor(seconds/60);if(minutes<60)return`${minutes}m ago`;const hours=Math.floor(minutes/60);if(hours<24)return`${hours}h ago`;return`${Math.floor(hours/24)}d ago`;}
function formatChartDate(value){const date=new Date(value);return Number.isNaN(date.getTime())?String(value):new Intl.DateTimeFormat(undefined,{month:"short",day:"numeric"}).format(date);}function updateUrl(id){const url=new URL(location.href);url.searchParams.set("organizationId",id);history.replaceState({},"",url);}function showMessage(text){pageMessage.textContent=text;pageMessage.classList.add("message-error");pageMessage.classList.remove("hidden");}function hideMessage(){pageMessage.classList.add("hidden");pageMessage.classList.remove("message-error");}

document.querySelectorAll(".scientific-tab").forEach(button=>button.addEventListener("click",async()=>{document.querySelectorAll(".scientific-tab").forEach(item=>item.classList.remove("active"));button.classList.add("active");await loadHistory(button.dataset.period);}));
document.querySelector("#refresh-overview").addEventListener("click",async event=>{if(refreshInProgress)return;refreshInProgress=true;event.currentTarget.classList.add("is-refreshing");try{await loadOverview(selectedOrganizationId,{silent:true});}finally{event.currentTarget.classList.remove("is-refreshing");refreshInProgress=false;}});
document.addEventListener("labmonitor:refresh",()=>{if(!refreshInProgress&&document.visibilityState==="visible")loadOverview(selectedOrganizationId,{silent:true});});
renderBreadcrumbs([{label:"Overview"}]);if(window.labMonitorOrganizationContext)initialize(window.labMonitorOrganizationContext.selected);else document.addEventListener("labmonitor:organization-ready",event=>initialize(event.detail),{once:true});document.addEventListener("labmonitor:organization-change",event=>{if(event.detail?.id&&event.detail.id!==selectedOrganizationId){configureRoleCopy(event.detail.id);loadOverview(event.detail.id);}});
