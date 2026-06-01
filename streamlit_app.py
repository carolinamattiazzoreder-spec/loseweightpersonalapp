import streamlit as st
import pandas as pd
import plotly.graph_objects as go
from datetime import datetime, timedelta
import json
import io
import os
from copy import deepcopy

import dropbox
import openpyxl
from openpyxl import Workbook

# ── Page config ───────────────────────────────────────────────────────────────
st.set_page_config(
    page_title="WeightTracker",
    page_icon="⚖️",
    layout="wide",
    initial_sidebar_state="collapsed",
)

st.markdown("""
<style>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap');
*, body, html { font-family: 'Plus Jakarta Sans', sans-serif !important; }
#MainMenu, footer, header { visibility: hidden; }
.stApp { background: #ffffff; min-height: 100vh; }

.block-container { padding: 0 !important; max-width: 100% !important; margin: 0 !important; }

/* Responsive page padding */
.page-body  { padding: 1.25rem clamp(0.75rem, 4vw, 2rem) 3rem; }
.inner-page { padding: 1.25rem clamp(0.75rem, 4vw, 2rem) 3rem; max-width: 640px; }

.nav-logo {
  font-size: clamp(0.8rem, 2.5vw, 0.95rem); font-weight: 800; letter-spacing: -0.02em;
  background: linear-gradient(135deg, #7c3aed, #f97316);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; white-space: nowrap;
}

/* Nav radio */
div[data-testid="stHorizontalBlock"] .stRadio > div {
  display: flex !important; flex-direction: row !important;
  gap: 0.2rem !important; flex-wrap: wrap !important;
}
.stRadio > div > label {
  background: #f9fafb !important; border: 1.5px solid #f0edf8 !important; border-radius: 9px !important;
  padding: 0.35rem 0.7rem !important; cursor: pointer !important;
  font-size: clamp(0.7rem, 2vw, 0.78rem) !important;
  font-weight: 600 !important; color: #9ca3af !important; transition: all 0.15s !important;
  white-space: nowrap !important;
}
.stRadio > div > label:has(input:checked) {
  background: linear-gradient(135deg,#7c3aed,#a855f7) !important; color: #fff !important;
  border-color: transparent !important; box-shadow: 0 2px 8px rgba(124,58,237,0.25) !important;
}
.stRadio [data-testid="stMarkdownContainer"] p { font-size: inherit !important; margin: 0 !important; }
.stRadio input[type="radio"] { display: none !important; }

/* Hero card */
.hero-card {
  background: linear-gradient(135deg, #faf5ff 0%, #fff7ed 100%);
  border-radius: 18px; border: 1.5px solid #ede9fe;
  padding: clamp(0.75rem, 2vw, 1.1rem) clamp(0.85rem, 2vw, 1.3rem);
  min-height: 120px;
}
.hero-lbl    { font-size: 0.62rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 0.25rem; }
.hero-weight { font-size: clamp(2rem, 6vw, 2.8rem); font-weight: 800; color: #1f2937; line-height: 1; letter-spacing: -0.04em; }
.hero-unit   { font-size: 0.95rem; font-weight: 600; color: #9ca3af; margin-left: 2px; }
.chip-good, .chip-bad, .chip-flat {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 0.68rem; font-weight: 700; padding: 2px 8px; border-radius: 99px; margin-top: 0.4rem;
}
.chip-good { background: #dcfce7; color: #16a34a; }
.chip-bad  { background: #fee2e2; color: #dc2626; }
.chip-flat { background: #f3f4f6; color: #6b7280; }

/* Stat cards */
.stat-card {
  border-radius: 18px;
  padding: clamp(0.75rem, 2vw, 1rem) clamp(0.85rem, 2vw, 1.1rem);
  min-height: 120px; display: flex; flex-direction: column; justify-content: space-between;
}
.sc-purple { background: linear-gradient(135deg, #7c3aed, #a855f7); }
.sc-orange { background: linear-gradient(135deg, #f97316, #fb923c); }
.sc-dark   { background: linear-gradient(135deg, #1e1b4b, #312e81); }
.sc-soft   { background: #f9fafb; border: 1.5px solid #f0edf8; }
.sc-lbl    { font-size: 0.62rem; font-weight: 700; color: rgba(255,255,255,0.62); text-transform: uppercase; letter-spacing: 0.07em; }
.sc-lbl-d  { font-size: 0.62rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.07em; }
.sc-val    { font-size: clamp(1rem, 3vw, 1.3rem); font-weight: 800; color: #fff; line-height: 1.1; letter-spacing: -0.02em; margin-top: 0.2rem; }
.sc-val-d  { font-size: clamp(1rem, 3vw, 1.3rem); font-weight: 800; color: #1f2937; line-height: 1.1; letter-spacing: -0.02em; margin-top: 0.2rem; }
.sc-sub    { font-size: 0.68rem; color: rgba(255,255,255,0.46); }
.sc-sub-d  { font-size: 0.68rem; color: #9ca3af; }

/* Progress bar */
.prog-wrap { background: #f3f0ff; border-radius: 99px; height: 8px; overflow: hidden; margin: 0.4rem 0 0.2rem; }
.prog-fill { height: 8px; border-radius: 99px; background: linear-gradient(90deg,#7c3aed,#f97316); }
.prog-row  { display: flex; justify-content: space-between; font-size: 0.65rem; color: #9ca3af; font-weight: 500; }

/* Chart */
.chart-wrap  { background: #faf5ff; border-radius: 18px; border: 1.5px solid #ede9fe; padding: 1rem 1rem 0.5rem; margin-bottom: 0.85rem; }
.chart-hint  { font-size: 0.65rem; color: #c4b5fd; text-align: center; margin-top: 0.2rem; }

.sec-lbl    { font-size: 0.62rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.09em; margin: 1rem 0 0.5rem; }
.small-note { font-size: 0.7rem; color: #9ca3af; margin-top: 0.3rem; }

/* Inputs */
.stTextInput>div>div>input, .stNumberInput>div>div>input {
  background: #f9fafb !important; border: 1.5px solid #e5e7eb !important; border-radius: 10px !important;
  color: #1f2937 !important; font-weight: 500 !important; font-size: 0.85rem !important;
}
.stTextInput>div>div>input:focus, .stNumberInput>div>div>input:focus {
  border-color: #7c3aed !important; background: #faf5ff !important;
  box-shadow: 0 0 0 3px rgba(124,58,237,0.12) !important;
}
label, .stTextInput label, .stNumberInput label, .stDateInput label,
.stSelectbox label, .stFileUploader label {
  color: #374151 !important; font-size: 0.78rem !important; font-weight: 600 !important;
}
.stButton>button {
  background: linear-gradient(135deg,#7c3aed,#a855f7) !important; color: #fff !important;
  border: none !important; border-radius: 10px !important; padding: 0.55rem 1.2rem !important;
  font-weight: 700 !important; font-size: 0.82rem !important; width: 100% !important;
  box-shadow: 0 3px 12px rgba(124,58,237,0.25) !important; transition: all 0.18s !important;
}
.stButton>button:hover { transform: translateY(-1px) !important; box-shadow: 0 5px 16px rgba(124,58,237,0.35) !important; }
.stDownloadButton>button {
  background: #fff7ed !important; color: #ea580c !important;
  border: 1.5px solid #fdba74 !important; box-shadow: none !important;
}
.stDateInput>div>div>input {
  background: #f9fafb !important; border: 1.5px solid #e5e7eb !important;
  border-radius: 10px !important; color: #1f2937 !important;
}
.stSelectbox>div>div {
  background: #f9fafb !important; border: 1.5px solid #e5e7eb !important;
  border-radius: 10px !important; color: #1f2937 !important;
}
.stSuccess { background: #f0fdf4 !important; border-color: #86efac !important; color: #16a34a !important; border-radius: 10px !important; }
.stError   { background: #fef2f2 !important; border-color: #fca5a5 !important; color: #dc2626 !important; border-radius: 10px !important; }
.stInfo    { background: #faf5ff !important; border-color: #c4b5fd !important; color: #7c3aed !important; border-radius: 10px !important; }
.stWarning { background: #fffbeb !important; border-color: #fcd34d !important; color: #b45309 !important; border-radius: 10px !important; }
.streamlit-expanderHeader { color: #374151 !important; font-size: 0.8rem !important; font-weight: 600 !important; }
details { background: #f9fafb !important; border-radius: 12px !important; border: 1.5px solid #f0edf8 !important; }
hr { border-color: #f0edf8 !important; }
.stDataFrame { border-radius: 12px !important; overflow: hidden !important; border: 1.5px solid #f0edf8 !important; }

@media (max-width: 480px) {
  .stat-row-wrap > div[data-testid="stHorizontalBlock"] { flex-wrap: wrap !important; }
  .stat-row-wrap > div[data-testid="stHorizontalBlock"] > div { min-width: 45% !important; flex: 1 1 45% !important; }
}
</style>
""", unsafe_allow_html=True)


# ── Constants ─────────────────────────────────────────────────────────────────
AUTO_BACKUP_HOURS = 16
DROPBOX_PATH      = "/WeightTrackerData.xlsx"
DROPBOX_BACKUP    = "/WeightTrackerData_backup.xlsx"


# ── Dropbox client ────────────────────────────────────────────────────────────
@st.cache_resource
def get_dropbox() -> dropbox.Dropbox:
    return dropbox.Dropbox(
        oauth2_refresh_token=st.secrets["dropbox"]["refresh_token"],
        app_key=st.secrets["dropbox"]["app_key"],
        app_secret=st.secrets["dropbox"]["app_secret"],
    )

# ── Utilities ─────────────────────────────────────────────────────────────────
def now_iso():
    return datetime.now().isoformat(timespec="seconds")


def default_state():
    return {
        "weights":        [],
        "goal_weight":    None,
        "height_cm":      170,
        "last_saved_at":  None,
        "last_backup_at": None,
    }


def normalize_weights(weights):
    """Deduplicate by date (last write wins), validate, sort ascending."""
    by_date = {}
    for item in weights or []:
        if not isinstance(item, dict):
            continue
        date_val   = str(item.get("date",   "")).strip()
        weight_val = item.get("weight")
        note_val   = str(item.get("note",   "")).strip()
        try:
            parsed_date   = datetime.strptime(date_val, "%Y-%m-%d").strftime("%Y-%m-%d")
            parsed_weight = round(float(weight_val), 1)
        except Exception:
            continue
        by_date[parsed_date] = {"date": parsed_date, "weight": parsed_weight, "note": note_val}
    return [v for _, v in sorted(by_date.items())]


def ensure_keys(d):
    base = default_state()
    if not isinstance(d, dict):
        d = {}
    base.update(d)
    base["weights"] = normalize_weights(base.get("weights", []))
    try:
        base["height_cm"] = int(base["height_cm"]) if base.get("height_cm") else 170
    except Exception:
        base["height_cm"] = 170
    try:
        gw = base.get("goal_weight")
        base["goal_weight"] = round(float(gw), 1) if gw not in (None, "") else None
    except Exception:
        base["goal_weight"] = None
    return base


# ── Excel ↔ state helpers ─────────────────────────────────────────────────────
def state_to_workbook(d: dict) -> Workbook:
    """Convert app state dict into a structured openpyxl Workbook."""
    wb = Workbook()

    # Sheet 1: Weights
    ws_w       = wb.active
    ws_w.title = "Weights"
    ws_w.append(["date", "weight", "note"])
    for entry in d.get("weights", []):
        ws_w.append([entry["date"], entry["weight"], entry.get("note", "")])

    # Sheet 2: Settings
    ws_s       = wb.create_sheet("Settings")
    ws_s.append(["key", "value"])
    for key in ["goal_weight", "height_cm", "last_saved_at", "last_backup_at"]:
        ws_s.append([key, str(d.get(key, "") or "")])

    return wb


def workbook_to_state(wb: Workbook) -> dict:
    """Parse a structured Workbook back into an app state dict."""
    state = default_state()

    if "Weights" in wb.sheetnames:
        ws_w = wb["Weights"]
        raw  = []
        for row in ws_w.iter_rows(min_row=2, values_only=True):
            if row and row[0]:
                raw.append({
                    "date":   str(row[0]).strip(),
                    "weight": row[1],
                    "note":   str(row[2]).strip() if row[2] else "",
                })
        state["weights"] = raw

    if "Settings" in wb.sheetnames:
        ws_s = wb["Settings"]
        for row in ws_s.iter_rows(min_row=2, values_only=True):
            if row and row[0]:
                state[str(row[0])] = row[1] if row[1] not in ("", "None", None) else None

    return ensure_keys(state)


def wb_to_bytes(wb: Workbook) -> bytes:
    buf = io.BytesIO()
    wb.save(buf)
    return buf.getvalue()


def bytes_to_wb(data: bytes) -> Workbook:
    return openpyxl.load_workbook(io.BytesIO(data))


# ── Load / Save ───────────────────────────────────────────────────────────────
def load_data() -> dict:
    """Download Excel from Dropbox and parse into app state."""
    try:
        dbx    = get_dropbox()
        _, res = dbx.files_download(DROPBOX_PATH)
        return workbook_to_state(bytes_to_wb(res.content))
    except dropbox.exceptions.ApiError as e:
        if "not_found" in str(e):
            return default_state()   # first run — file doesn't exist yet
        st.warning(f"⚠️ Could not load from Dropbox: {e}")
    except Exception as e:
        st.warning(f"⚠️ Unexpected load error: {e}")
    return default_state()


def save_data(d: dict) -> dict:
    """Upload Excel to Dropbox (overwrites previous version)."""
    payload                  = ensure_keys(deepcopy(d))
    payload["last_saved_at"] = now_iso()
    try:
        dbx  = get_dropbox()
        data = wb_to_bytes(state_to_workbook(payload))
        dbx.files_upload(data, DROPBOX_PATH, mode=dropbox.files.WriteMode.overwrite)
    except Exception as e:
        st.error(f"❌ Save failed: {e}")
    st.session_state.app_state = payload
    return payload


def maybe_auto_backup(d: dict):
    state         = ensure_keys(deepcopy(d))
    last_backup   = state.get("last_backup_at")
    should_backup = True
    if last_backup:
        try:
            last_dt       = datetime.fromisoformat(last_backup)
            should_backup = (datetime.now() - last_dt) >= timedelta(hours=AUTO_BACKUP_HOURS)
        except Exception:
            should_backup = True
    if should_backup:
        backup_payload                        = deepcopy(state)
        backup_payload["backup_generated_at"] = now_iso()
        try:
            dbx  = get_dropbox()
            data = wb_to_bytes(state_to_workbook(backup_payload))
            dbx.files_upload(data, DROPBOX_BACKUP, mode=dropbox.files.WriteMode.overwrite)
        except Exception:
            pass
        state["last_backup_at"] = now_iso()
        state = save_data(state)
        return state, True
    return state, False


# ── Import / Export ───────────────────────────────────────────────────────────
def export_json_bytes(d):
    return json.dumps(ensure_keys(d), indent=2, ensure_ascii=False).encode("utf-8")


def import_json_bytes(uploaded_bytes):
    return ensure_keys(json.loads(uploaded_bytes.decode("utf-8")))


# ── Analytics helpers ─────────────────────────────────────────────────────────
def calc_bmi(w, h):
    if not h or h <= 0:
        return None
    return round(w / (h / 100) ** 2, 1)


def bmi_cat(b):
    if b < 18.5: return "Underweight"
    if b < 25:   return "Normal weight"
    if b < 30:   return "Overweight"
    return "Obese"


def bmi_color(b):
    if b < 18.5: return "#2563eb"
    if b < 25:   return "#16a34a"
    if b < 30:   return "#ea580c"
    return "#dc2626"


def weeks_to_goal(cur, goal, rate=1.0):
    d = cur - goal
    return round(d / rate, 1) if d > 0 else None


def eta_date(w):
    return (datetime.today() + timedelta(weeks=w)).strftime("%b %d, %Y")


def weight_trend_label(weights):
    if len(weights) < 2:
        return None
    recent = [x["weight"] for x in weights[-7:]]
    if len(recent) < 2:
        return None
    delta = recent[-1] - recent[0]
    if abs(delta) < 0.1:
        return "Stable this week"
    direction = "▼" if delta < 0 else "▲"
    return f"{direction} {abs(delta):.1f} kg this week"


# ── Chart builder ─────────────────────────────────────────────────────────────
def build_chart(weights, goal_weight, y_tick_step=5):
    df            = pd.DataFrame(weights).sort_values("date")
    df["date"]    = pd.to_datetime(df["date"])
    baseline_val  = df["weight"].min() - 4
    y_min         = baseline_val
    y_max         = df["weight"].max() + 2
    fig           = go.Figure()

    # Filled area
    fig.add_trace(go.Scatter(
        x=df["date"], y=[baseline_val] * len(df),
        mode="lines", line=dict(color="rgba(0,0,0,0)", width=0),
        showlegend=False, hoverinfo="skip",
    ))
    fig.add_trace(go.Scatter(
        x=df["date"], y=df["weight"],
        fill="tonexty", fillcolor="rgba(124,58,237,0.13)",
        line=dict(color="rgba(0,0,0,0)", width=0),
        showlegend=False, hoverinfo="skip",
    ))

    # Goal projection
    all_x = list(df["date"])
    if goal_weight:
        last_date = df["date"].iloc[-1]
        last_w    = df["weight"].iloc[-1]
        weeks     = weeks_to_goal(last_w, goal_weight)
        if weeks and weeks > 0:
            n_pts      = int(weeks) + 2
            proj_dates = pd.date_range(last_date, periods=n_pts, freq="7D")
            proj_vals  = [max(last_w - i, goal_weight) for i in range(n_pts)]
            fig.add_trace(go.Scatter(
                x=proj_dates, y=[baseline_val] * len(proj_dates),
                mode="lines", line=dict(color="rgba(0,0,0,0)", width=0),
                showlegend=False, hoverinfo="skip",
            ))
            fig.add_trace(go.Scatter(
                x=proj_dates, y=proj_vals,
                fill="tonexty", fillcolor="rgba(16,185,129,0.10)",
                line=dict(color="#10b981", width=2.5, dash="dot"),
                name="Projection (−1 kg/wk)",
                hovertemplate="<b>%{y:.1f} kg</b> projected<br>%{x|%b %d, %Y}<extra></extra>",
            ))
            all_x.append(last_date + timedelta(weeks=int(weeks) + 1))

    # Actual line
    fig.add_trace(go.Scatter(
        x=df["date"], y=df["weight"],
        mode="lines+markers",
        line=dict(color="#7c3aed", width=3, shape="spline"),
        marker=dict(size=7, color="#fff", line=dict(color="#7c3aed", width=2.5)),
        name="Weight",
        hovertemplate="<b>%{y} kg</b>  •  %{x|%b %d, %Y}<extra></extra>",
    ))

    if goal_weight:
        fig.add_hline(
            y=goal_weight,
            line=dict(color="#f97316", width=1.5, dash="dash"),
            annotation_text=f"  🎯 {goal_weight} kg",
            annotation_position="bottom right",
            annotation_font=dict(color="#f97316", size=10, family="Plus Jakarta Sans"),
        )

    fig.update_layout(
        paper_bgcolor="rgba(0,0,0,0)", plot_bgcolor="rgba(0,0,0,0)",
        margin=dict(l=0, r=4, t=8, b=0), height=320,
        xaxis=dict(
            showgrid=False, zeroline=False,
            tickfont=dict(color="#9ca3af", size=10, family="Plus Jakarta Sans"),
            tickformat="%b %d", showline=False,
            spikecolor="#c4b5fd", spikethickness=1, spikemode="across",
            range=[df["date"].min(), all_x[-1]] if all_x else None, dtick="M1",
        ),
        yaxis=dict(
            showgrid=True, gridcolor="rgba(124,58,237,0.07)", zeroline=False,
            tickfont=dict(color="#9ca3af", size=10, family="Plus Jakarta Sans"),
            ticksuffix=" kg", dtick=y_tick_step, range=[y_min, y_max],
        ),
        legend=dict(
            font=dict(color="#6b7280", size=10, family="Plus Jakarta Sans"),
            bgcolor="rgba(0,0,0,0)", orientation="h", y=-0.18,
        ),
        hovermode="x unified",
        hoverlabel=dict(
            bgcolor="#1f2937",
            font=dict(color="#fff", size=11, family="Plus Jakarta Sans"),
            bordercolor="#374151",
        ),
        dragmode="pan",
    )
    return fig


# ── Session-state bootstrap ───────────────────────────────────────────────────
if "app_state" not in st.session_state:
    st.session_state.app_state = load_data()

state = st.session_state.app_state
state, backup_created = maybe_auto_backup(state)
st.session_state.app_state = state


# ── Navigation ────────────────────────────────────────────────────────────────
nav_left, nav_mid, nav_right = st.columns([2, 5, 2])
with nav_left:
    st.markdown(
        '<div style="padding:0.55rem 0 0 clamp(0.5rem,3vw,1.5rem)">'
        '<span class="nav-logo">⚖️ WeightTracker</span></div>',
        unsafe_allow_html=True,
    )
with nav_mid:
    page = st.radio(
        "nav",
        ["📊 Dashboard", "➕ Log Weight", "⚙️ Settings"],
        horizontal=True,
        label_visibility="collapsed",
    )
with nav_right:
    st.markdown(
        '<div style="text-align:right;font-size:0.7rem;color:#9ca3af;'
        'font-weight:600;padding:0.6rem clamp(0.5rem,3vw,1.4rem) 0 0">Personal tracker</div>',
        unsafe_allow_html=True,
    )

st.markdown('<hr style="margin:0">', unsafe_allow_html=True)

if backup_created:
    st.info(f"✅ Auto-backup saved to Dropbox. Next in ~{AUTO_BACKUP_HOURS} hours of use.")


# ══════════════════════════════════════════════════════════════════════════════
# DASHBOARD
# ══════════════════════════════════════════════════════════════════════════════
if page == "📊 Dashboard":
    weights = state.get("weights", [])
    goal    = state.get("goal_weight")
    height  = state.get("height_cm")

    st.markdown('<div class="page-body">', unsafe_allow_html=True)

    if not weights:
        st.markdown(
            '<div style="text-align:center;padding:4rem 1rem">'
            '<div style="font-size:3rem">📭</div>'
            '<div style="font-size:1rem;font-weight:700;color:#1f2937;margin-top:0.5rem">No entries yet</div>'
            '<div style="font-size:0.8rem;color:#9ca3af;margin-top:0.2rem">Go to <b>Log Weight</b> to start tracking</div>'
            '</div>',
            unsafe_allow_html=True,
        )
    else:
        df           = pd.DataFrame(weights).sort_values("date")
        cw           = df["weight"].iloc[-1]
        sw           = df["weight"].iloc[0]
        prev_w       = df["weight"].iloc[-2] if len(df) > 1 else cw
        delta        = round(cw - prev_w, 2)
        total_change = round(sw - cw, 2)
        bmi_val      = calc_bmi(cw, height)
        weeks        = weeks_to_goal(cw, goal) if goal else None
        rem          = round(cw - goal, 1) if goal else None

        # ── Top row ──────────────────────────────────────────────────────────
        top_a, top_b, top_c = st.columns([1.35, 1, 1])

        with top_a:
            if delta < 0:
                chip = f'<div class="chip-good">▼ {abs(delta):.2f} kg</div>'
            elif delta > 0:
                chip = f'<div class="chip-bad">▲ {abs(delta):.2f} kg</div>'
            else:
                chip = '<div class="chip-flat">— no change</div>'
            st.markdown(
                f'<div class="hero-card">'
                f'<div class="hero-lbl">Current Weight</div>'
                f'<div class="hero-weight">{cw}<span class="hero-unit"> kg</span></div>'
                f'{chip}</div>',
                unsafe_allow_html=True,
            )

        with top_b:
            change_icon  = "▼" if total_change > 0 else "▲" if total_change < 0 else "—"
            change_color = "#16a34a" if total_change > 0 else "#dc2626" if total_change < 0 else "#6b7280"
            st.markdown(
                f'<div class="stat-card sc-purple">'
                f'<div class="sc-lbl">Total Change</div>'
                f'<div class="sc-val">{change_icon} {abs(total_change):.1f} kg</div>'
                f'<div class="sc-sub">since {df["date"].iloc[0]}</div>'
                f'</div>',
                unsafe_allow_html=True,
            )

        with top_c:
            bmi_display = f"{bmi_val} · {bmi_cat(bmi_val)}" if bmi_val else "Set height first"
            st.markdown(
                f'<div class="stat-card sc-orange">'
                f'<div class="sc-lbl">BMI</div>'
                f'<div class="sc-val">{bmi_display}</div>'
                f'<div class="sc-sub">{height} cm</div>'
                f'</div>',
                unsafe_allow_html=True,
            )

        st.markdown("<div style='height:0.6rem'></div>", unsafe_allow_html=True)

        # ── Goal progress row ─────────────────────────────────────────────────
        if goal:
            g_a, g_b, g_c = st.columns(3)
            progress      = max(0, min(100, int(((sw - cw) / (sw - goal)) * 100))) if sw != goal else 100

            with g_a:
                st.markdown(
                    f'<div class="stat-card sc-dark">'
                    f'<div class="sc-lbl">Goal Weight</div>'
                    f'<div class="sc-val">🎯 {goal} kg</div>'
                    f'<div class="sc-sub">{rem:+.1f} kg remaining</div>'
                    f'</div>',
                    unsafe_allow_html=True,
                )
            with g_b:
                eta = eta_date(weeks) if weeks else "—"
                st.markdown(
                    f'<div class="stat-card sc-soft">'
                    f'<div class="sc-lbl-d">Est. Arrival</div>'
                    f'<div class="sc-val-d">📅 {eta}</div>'
                    f'<div class="sc-sub-d">at −1 kg/week</div>'
                    f'</div>',
                    unsafe_allow_html=True,
                )
            with g_c:
                st.markdown(
                    f'<div class="stat-card sc-soft">'
                    f'<div class="sc-lbl-d">Progress</div>'
                    f'<div class="sc-val-d">{progress}%</div>'
                    f'<div class="prog-wrap"><div class="prog-fill" style="width:{progress}%"></div></div>'
                    f'<div class="prog-row"><span>{sw} kg</span><span>{goal} kg</span></div>'
                    f'</div>',
                    unsafe_allow_html=True,
                )
            st.markdown("<div style='height:0.6rem'></div>", unsafe_allow_html=True)

        # ── Chart ─────────────────────────────────────────────────────────────
        trend = weight_trend_label(weights)
        st.markdown(
            f'<div class="sec-lbl">Weight History{(" — " + trend) if trend else ""}</div>',
            unsafe_allow_html=True,
        )
        st.markdown('<div class="chart-wrap">', unsafe_allow_html=True)
        if len(weights) >= 2:
            fig = build_chart(weights, goal)
            st.plotly_chart(fig, use_container_width=True, config={"displayModeBar": False})
            st.markdown('<div class="chart-hint">Drag to pan • scroll to zoom</div>', unsafe_allow_html=True)
        else:
            st.info("Add at least 2 entries to see your chart.")
        st.markdown("</div>", unsafe_allow_html=True)

        # ── Log table ─────────────────────────────────────────────────────────
        st.markdown('<div class="sec-lbl">All Entries</div>', unsafe_allow_html=True)
        display_df = df[["date", "weight", "note"]].copy().sort_values("date", ascending=False)
        display_df.columns = ["Date", "Weight (kg)", "Note"]
        st.dataframe(display_df, use_container_width=True, hide_index=True)

    st.markdown("</div>", unsafe_allow_html=True)


# ══════════════════════════════════════════════════════════════════════════════
# LOG WEIGHT
# ══════════════════════════════════════════════════════════════════════════════
elif page == "➕ Log Weight":
    st.markdown('<div class="inner-page">', unsafe_allow_html=True)
    st.markdown('<div class="sec-lbl">Log a Weight Entry</div>', unsafe_allow_html=True)

    with st.form("log_form", clear_on_submit=True):
        col_d, col_w = st.columns(2)
        with col_d:
            entry_date = st.date_input("Date", value=datetime.today())
        with col_w:
            entry_weight = st.number_input("Weight (kg)", min_value=20.0, max_value=300.0,
                                           value=80.0, step=0.1, format="%.1f")
        entry_note = st.text_input("Note (optional)", placeholder="e.g. After workout, morning weigh-in…")
        submitted  = st.form_submit_button("💾 Save Entry")

    if submitted:
        new_entry = {
            "date":   entry_date.strftime("%Y-%m-%d"),
            "weight": round(float(entry_weight), 1),
            "note":   entry_note.strip(),
        }
        current = deepcopy(st.session_state.app_state)
        current["weights"].append(new_entry)
        st.session_state.app_state = save_data(current)
        st.success(f"✅ Saved {new_entry['weight']} kg on {new_entry['date']}")

    # ── Quick-edit: delete an entry ───────────────────────────────────────────
    weights = st.session_state.app_state.get("weights", [])
    if weights:
        st.markdown('<div class="sec-lbl" style="margin-top:1.5rem">Delete an Entry</div>', unsafe_allow_html=True)
        date_options = [w["date"] for w in sorted(weights, key=lambda x: x["date"], reverse=True)]
        del_date     = st.selectbox("Select date to delete", date_options)
        if st.button("🗑️ Delete selected entry"):
            current            = deepcopy(st.session_state.app_state)
            current["weights"] = [w for w in current["weights"] if w["date"] != del_date]
            st.session_state.app_state = save_data(current)
            st.success(f"Deleted entry for {del_date}")
            st.rerun()

    st.markdown("</div>", unsafe_allow_html=True)


# ══════════════════════════════════════════════════════════════════════════════
# SETTINGS
# ══════════════════════════════════════════════════════════════════════════════
elif page == "⚙️ Settings":
    st.markdown('<div class="inner-page">', unsafe_allow_html=True)

    # ── Profile ───────────────────────────────────────────────────────────────
    st.markdown('<div class="sec-lbl">Profile</div>', unsafe_allow_html=True)
    with st.form("profile_form"):
        col_h, col_g = st.columns(2)
        with col_h:
            new_height = st.number_input(
                "Height (cm)",
                min_value=100, max_value=250,
                value=int(state.get("height_cm") or 170),
                step=1,
            )
        with col_g:
            gw_default = float(state["goal_weight"]) if state.get("goal_weight") else 70.0
            new_goal   = st.number_input(
                "Goal weight (kg)",
                min_value=20.0, max_value=300.0,
                value=gw_default,
                step=0.5, format="%.1f",
            )
        save_profile = st.form_submit_button("💾 Save Profile")

    if save_profile:
        current                 = deepcopy(st.session_state.app_state)
        current["height_cm"]    = int(new_height)
        current["goal_weight"]  = round(float(new_goal), 1)
        st.session_state.app_state = save_data(current)
        st.success("✅ Profile saved!")

    # ── Export ────────────────────────────────────────────────────────────────
    st.markdown('<div class="sec-lbl" style="margin-top:1.5rem">Export Data</div>', unsafe_allow_html=True)
    json_bytes = export_json_bytes(st.session_state.app_state)
    st.download_button(
        label="⬇️ Download backup (JSON)",
        data=json_bytes,
        file_name=f"weighttracker_backup_{datetime.today().strftime('%Y%m%d')}.json",
        mime="application/json",
    )

    # Also offer Excel download
    wb_bytes = wb_to_bytes(state_to_workbook(ensure_keys(st.session_state.app_state)))
    st.download_button(
        label="⬇️ Download as Excel (.xlsx)",
        data=wb_bytes,
        file_name=f"weighttracker_{datetime.today().strftime('%Y%m%d')}.xlsx",
        mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )

    # ── Import ────────────────────────────────────────────────────────────────
    st.markdown('<div class="sec-lbl" style="margin-top:1.5rem">Import Data</div>', unsafe_allow_html=True)
    st.markdown('<div class="small-note">Upload a previously exported JSON backup to restore your data.</div>', unsafe_allow_html=True)
    uploaded = st.file_uploader("Upload JSON backup", type=["json"], label_visibility="collapsed")
    if uploaded:
        if st.button("📥 Import & overwrite current data"):
            try:
                imported = import_json_bytes(uploaded.read())
                st.session_state.app_state = save_data(imported)
                st.success("✅ Data imported and saved to Dropbox!")
                st.rerun()
            except Exception as e:
                st.error(f"Import failed: {e}")

    # ── Danger zone ───────────────────────────────────────────────────────────
    st.markdown('<div class="sec-lbl" style="margin-top:1.5rem;color:#dc2626">Danger Zone</div>', unsafe_allow_html=True)
    with st.expander("⚠️ Clear all weight entries"):
        st.warning("This will permanently delete all logged entries. Your profile settings (height, goal) will be kept.")
        if st.button("🗑️ Delete all entries"):
            current            = deepcopy(st.session_state.app_state)
            current["weights"] = []
            st.session_state.app_state = save_data(current)
            st.success("All entries deleted.")
            st.rerun()

    # ── Storage info ──────────────────────────────────────────────────────────
    st.markdown('<div class="sec-lbl" style="margin-top:1.5rem">Storage Info</div>', unsafe_allow_html=True)
    last_saved  = state.get("last_saved_at") or "Never"
    last_backup = state.get("last_backup_at") or "Never"
    entry_count = len(state.get("weights", []))
    st.markdown(
        f'<div style="background:#f9fafb;border:1.5px solid #f0edf8;border-radius:12px;padding:0.85rem 1rem">'
        f'<div style="font-size:0.75rem;color:#374151;font-weight:600;margin-bottom:0.4rem">📦 Dropbox · WeightTrackerData.xlsx</div>'
        f'<div style="font-size:0.7rem;color:#6b7280">Entries: <b>{entry_count}</b></div>'
        f'<div style="font-size:0.7rem;color:#6b7280">Last saved: <b>{last_saved}</b></div>'
        f'<div style="font-size:0.7rem;color:#6b7280">Last backup: <b>{last_backup}</b></div>'
        f'</div>',
        unsafe_allow_html=True,
    )

    st.markdown("</div>", unsafe_allow_html=True)
