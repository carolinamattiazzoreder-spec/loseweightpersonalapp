import streamlit as st
import pandas as pd
import plotly.graph_objects as go
from datetime import datetime, timedelta
import json
import os
from copy import deepcopy

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
.page-body { padding: 1.25rem clamp(0.75rem, 4vw, 2rem) 3rem; }
.inner-page { padding: 1.25rem clamp(0.75rem, 4vw, 2rem) 3rem; max-width: 640px; }

.nav-logo {
  font-size: clamp(0.8rem, 2.5vw, 0.95rem); font-weight: 800; letter-spacing: -0.02em;
  background: linear-gradient(135deg, #7c3aed, #f97316);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; white-space: nowrap;
}

/* Nav radio — wraps gracefully on mobile */
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
.hero-lbl { font-size: 0.62rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 0.25rem; }
.hero-weight { font-size: clamp(2rem, 6vw, 2.8rem); font-weight: 800; color: #1f2937; line-height: 1; letter-spacing: -0.04em; }
.hero-unit { font-size: 0.95rem; font-weight: 600; color: #9ca3af; margin-left: 2px; }
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
.chart-wrap { background: #faf5ff; border-radius: 18px; border: 1.5px solid #ede9fe; padding: 1rem 1rem 0.5rem; margin-bottom: 0.85rem; }
.chart-hint { font-size: 0.65rem; color: #c4b5fd; text-align: center; margin-top: 0.2rem; }

.sec-lbl { font-size: 0.62rem; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: 0.09em; margin: 1rem 0 0.5rem; }
.small-note { font-size: 0.7rem; color: #9ca3af; margin-top: 0.3rem; }

/* Inputs */
.stTextInput>div>div>input,
.stNumberInput>div>div>input {
  background: #f9fafb !important; border: 1.5px solid #e5e7eb !important; border-radius: 10px !important;
  color: #1f2937 !important; font-weight: 500 !important; font-size: 0.85rem !important;
}
.stTextInput>div>div>input:focus,
.stNumberInput>div>div>input:focus {
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

/* Mobile: stack two-column grids */
@media (max-width: 480px) {
  .stat-row-wrap > div[data-testid="stHorizontalBlock"] { flex-wrap: wrap !important; }
  .stat-row-wrap > div[data-testid="stHorizontalBlock"] > div { min-width: 45% !important; flex: 1 1 45% !important; }
}
</style>
""", unsafe_allow_html=True)

# ── Constants ──────────────────────────────────────────────────────────────────
DATA_FILE = "weight_data.json"
BACKUP_FILE = "weight_data_autobackup.json"
AUTO_BACKUP_HOURS = 16


# ── Utilities ──────────────────────────────────────────────────────────────────
def now_iso():
    return datetime.now().isoformat(timespec="seconds")


def default_state():
    return {
        "weights": [],
        "goal_weight": None,
        "height_cm": 170,
        "last_saved_at": None,
        "last_backup_at": None,
    }


def normalize_weights(weights):
    """Deduplicate by date (last write wins), validate, and sort ascending."""
    by_date = {}
    for item in weights or []:
        if not isinstance(item, dict):
            continue
        date_val = str(item.get("date", "")).strip()
        weight_val = item.get("weight")
        note_val = str(item.get("note", "")).strip()
        try:
            parsed_date = datetime.strptime(date_val, "%Y-%m-%d").strftime("%Y-%m-%d")
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


def load_data():
    if os.path.exists(DATA_FILE):
        try:
            with open(DATA_FILE, encoding="utf-8") as f:
                return ensure_keys(json.load(f))
        except Exception:
            pass
    return default_state()


def atomic_write_json(path, payload):
    """Write JSON atomically: write to .tmp then os.replace to avoid corruption."""
    temp_path = f"{path}.tmp"
    with open(temp_path, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)
        f.flush()
        os.fsync(f.fileno())
    os.replace(temp_path, path)


def save_data(d):
    """Persist state to disk and return the updated state dict."""
    payload = ensure_keys(deepcopy(d))
    payload["last_saved_at"] = now_iso()
    atomic_write_json(DATA_FILE, payload)
    # Keep session_state in sync
    st.session_state.app_state = payload
    return payload


def maybe_auto_backup(d):
    state = ensure_keys(deepcopy(d))
    last_backup = state.get("last_backup_at")
    should_backup = True
    if last_backup:
        try:
            last_dt = datetime.fromisoformat(last_backup)
            should_backup = (datetime.now() - last_dt) >= timedelta(hours=AUTO_BACKUP_HOURS)
        except Exception:
            should_backup = True
    if should_backup:
        backup_payload = deepcopy(state)
        backup_payload["backup_generated_at"] = now_iso()
        try:
            atomic_write_json(BACKUP_FILE, backup_payload)
        except Exception:
            pass
        state["last_backup_at"] = now_iso()
        state = save_data(state)
        return state, True
    return state, False


def export_json_bytes(d):
    return json.dumps(ensure_keys(d), indent=2, ensure_ascii=False).encode("utf-8")


def import_json_bytes(uploaded_bytes):
    parsed = json.loads(uploaded_bytes.decode("utf-8"))
    return ensure_keys(parsed)


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
    """Weeks at 1 kg/week loss rate. Returns None if already at/below goal."""
    d = cur - goal
    return round(d / rate, 1) if d > 0 else None


def eta_date(w):
    return (datetime.today() + timedelta(weeks=w)).strftime("%b %d, %Y")


def weight_trend_label(weights):
    """Simple 7-day trend description."""
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


def build_chart(weights, goal_weight, y_tick_step=5):
    df = pd.DataFrame(weights).sort_values("date")
    df["date"] = pd.to_datetime(df["date"])

    baseline_val = df["weight"].min() - 4
    y_min = baseline_val
    y_max = df["weight"].max() + 2
    fig = go.Figure()

    # Filled area under the actual line
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

    # Projection towards goal
    all_x = list(df["date"])
    if goal_weight:
        last_date = df["date"].iloc[-1]
        last_w = df["weight"].iloc[-1]
        weeks = weeks_to_goal(last_w, goal_weight)
        if weeks and weeks > 0:
            n_pts = int(weeks) + 2
            proj_dates = pd.date_range(last_date, periods=n_pts, freq="7D")
            proj_vals = [max(last_w - i, goal_weight) for i in range(n_pts)]

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

    # Actual weight line
    fig.add_trace(go.Scatter(
        x=df["date"], y=df["weight"],
        mode="lines+markers",
        line=dict(color="#7c3aed", width=3, shape="spline"),
        marker=dict(size=7, color="#fff", line=dict(color="#7c3aed", width=2.5)),
        name="Weight",
        hovertemplate="<b>%{y} kg</b>  •  %{x|%b %d, %Y}<extra></extra>",
    ))

    # Goal line
    if goal_weight:
        fig.add_hline(
            y=goal_weight,
            line=dict(color="#f97316", width=1.5, dash="dash"),
            annotation_text=f"  🎯 {goal_weight} kg",
            annotation_position="bottom right",
            annotation_font=dict(color="#f97316", size=10, family="Plus Jakarta Sans"),
        )

    fig.update_layout(
        paper_bgcolor="rgba(0,0,0,0)",
        plot_bgcolor="rgba(0,0,0,0)",
        margin=dict(l=0, r=4, t=8, b=0),
        height=320,
        xaxis=dict(
            showgrid=False, zeroline=False,
            tickfont=dict(color="#9ca3af", size=10, family="Plus Jakarta Sans"),
            tickformat="%b %d", showline=False,
            spikecolor="#c4b5fd", spikethickness=1, spikemode="across",
            range=[df["date"].min(), all_x[-1]] if all_x else None,
            dtick="M1",
        ),
        yaxis=dict(
            showgrid=True, gridcolor="rgba(124,58,237,0.07)",
            zeroline=False,
            tickfont=dict(color="#9ca3af", size=10, family="Plus Jakarta Sans"),
            ticksuffix=" kg", dtick=y_tick_step,
            range=[y_min, y_max],
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


# ── Session-state bootstrap ────────────────────────────────────────────────────
# FIX: All state lives in st.session_state so it survives Streamlit reruns.
# load_data() only runs once per session; every save() keeps it in sync.
if "app_state" not in st.session_state:
    st.session_state.app_state = load_data()

state = st.session_state.app_state

# Auto-backup (only touches disk if interval has elapsed)
state, backup_created = maybe_auto_backup(state)
st.session_state.app_state = state


# ── Navigation bar ─────────────────────────────────────────────────────────────
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
    st.info(f"✅ Auto-backup refreshed. Next in ~{AUTO_BACKUP_HOURS} hours of use.")


# ── Dashboard ──────────────────────────────────────────────────────────────────
if page == "📊 Dashboard":
    weights = state.get("weights", [])
    goal = state.get("goal_weight")
    height = state.get("height_cm")

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
        df = pd.DataFrame(weights).sort_values("date")
        cw = df["weight"].iloc[-1]          # current (latest) weight
        sw = df["weight"].iloc[0]           # starting weight
        prev_w = df["weight"].iloc[-2] if len(df) > 1 else cw
        delta = round(cw - prev_w, 2)

        # FIX: compute net change directionally (not just abs loss)
        total_change = round(sw - cw, 2)    # positive = loss, negative = gain
        bmi_val = calc_bmi(cw, height)
        weeks = weeks_to_goal(cw, goal) if goal else None
        rem = round(cw - goal, 1) if goal else None

        # ── Top row: hero + two stat cards ────────────────────────────────────
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
            # FIX: show "Total Lost" or "Total Gained" based on direction
            if total_change >= 0:
                label, val_str = "Total Lost", f"{total_change:.1f} kg"
                card_class = "sc-purple"
            else:
                label, val_str = "Total Gained", f"+{abs(total_change):.1f} kg"
                card_class = "sc-orange"
            st.markdown(
                f'<div class="stat-card {card_class}">'
                f'<div><div class="sc-lbl">{label}</div>'
                f'<div class="sc-val">{val_str}</div></div>'
                f'<div class="sc-sub">since first entry</div></div>',
                unsafe_allow_html=True,
            )

        with top_c:
            if goal and rem is not None:
                if rem > 0:
                    st.markdown(
                        f'<div class="stat-card sc-orange">'
                        f'<div><div class="sc-lbl">Remaining</div>'
                        f'<div class="sc-val">{rem} kg</div></div>'
                        f'<div class="sc-sub">to goal of {goal} kg</div></div>',
                        unsafe_allow_html=True,
                    )
                else:
                    st.markdown(
                        '<div class="stat-card sc-purple">'
                        '<div><div class="sc-lbl">Goal</div>'
                        '<div class="sc-val">🎉 Done!</div></div>'
                        f'<div class="sc-sub">{goal} kg reached</div></div>',
                        unsafe_allow_html=True,
                    )
            else:
                st.markdown(
                    f'<div class="stat-card sc-soft">'
                    f'<div><div class="sc-lbl-d">Entries</div>'
                    f'<div class="sc-val-d">{len(weights)}</div></div>'
                    f'<div class="sc-sub-d">weigh-ins logged</div></div>',
                    unsafe_allow_html=True,
                )

        # ── Bottom row ─────────────────────────────────────────────────────────
        bot_a, bot_b, bot_c = st.columns([1.35, 1, 1])

        with bot_a:
            if goal and sw > goal:
                pct = min(100, max(0, round((sw - cw) / (sw - goal) * 100, 1)))
                trend = weight_trend_label(weights)
                trend_html = f'<div class="small-note" style="margin-top:0.3rem">{trend}</div>' if trend else ""
                st.markdown(
                    f'<div style="padding-top:0.15rem">'
                    f'<div style="display:flex;justify-content:space-between;margin-bottom:4px">'
                    f'<span style="font-size:0.62rem;font-weight:700;color:#9ca3af;text-transform:uppercase;letter-spacing:0.08em">Progress to goal</span>'
                    f'<span style="font-size:0.78rem;font-weight:800;background:linear-gradient(135deg,#7c3aed,#f97316);-webkit-background-clip:text;-webkit-text-fill-color:transparent">{pct}%</span>'
                    f'</div>'
                    f'<div class="prog-wrap"><div class="prog-fill" style="width:{pct}%"></div></div>'
                    f'<div class="prog-row"><span>{sw} kg start</span><span>{goal} kg goal</span></div>'
                    f'{trend_html}</div>',
                    unsafe_allow_html=True,
                )

        with bot_b:
            if goal and weeks and weeks > 0:
                st.markdown(
                    f'<div class="stat-card sc-dark">'
                    f'<div><div class="sc-lbl">Goal Date</div>'
                    f'<div class="sc-val" style="font-size:clamp(0.85rem,2.5vw,1rem)">{eta_date(weeks)}</div></div>'
                    f'<div class="sc-sub">{weeks} wks at −1 kg/wk</div></div>',
                    unsafe_allow_html=True,
                )

        with bot_c:
            if bmi_val:
                bmi_c = bmi_color(bmi_val)
                st.markdown(
                    f'<div class="stat-card sc-soft">'
                    f'<div><div class="sc-lbl-d">BMI</div>'
                    f'<div class="sc-val-d" style="color:{bmi_c}">{bmi_val}</div></div>'
                    f'<div class="sc-sub-d">{bmi_cat(bmi_val)}</div></div>',
                    unsafe_allow_html=True,
                )

        # ── Chart ──────────────────────────────────────────────────────────────
        st.markdown('<div class="sec-lbl">Weight Over Time</div>', unsafe_allow_html=True)
        tick_choice = st.selectbox(
            "Graph scale",
            options=[1, 2, 5, 10],
            index=2,
            format_func=lambda x: f"{x} kg intervals",
            key="graph_tick_step",
        )
        st.markdown('<div class="chart-wrap">', unsafe_allow_html=True)
        fig = build_chart(weights, goal, y_tick_step=tick_choice)
        st.plotly_chart(
            fig,
            use_container_width=True,
            config={
                "displayModeBar": True,
                "scrollZoom": True,
                "modeBarButtonsToRemove": ["select2d", "lasso2d", "autoScale2d", "resetScale2d", "toImage"],
                "displaylogo": False,
            },
        )
        st.markdown('</div>', unsafe_allow_html=True)
        st.markdown('<div class="chart-hint">Scroll to zoom · drag to pan · hover for values</div>', unsafe_allow_html=True)

        # ── All entries table (includes notes) ────────────────────────────────
        with st.expander("📋 All entries"):
            df_show = df[["date", "weight", "note"]].copy()
            df_show.columns = ["Date", "Weight (kg)", "Note"]
            df_show = df_show.sort_values("Date", ascending=False).reset_index(drop=True)
            st.dataframe(df_show, use_container_width=True, hide_index=True)

    st.markdown('</div>', unsafe_allow_html=True)


# ── Log Weight ─────────────────────────────────────────────────────────────────
elif page == "➕ Log Weight":
    st.markdown('<div class="inner-page">', unsafe_allow_html=True)
    st.markdown('<div class="sec-lbl">New Entry</div>', unsafe_allow_html=True)

    with st.form("log_form", clear_on_submit=True):
        d = st.date_input("Date", value=datetime.today())
        w = st.number_input(
            "Weight (kg)", min_value=20.0, max_value=300.0,
            value=70.0, step=0.1, format="%.1f",
        )
        n = st.text_input("Note (optional)", placeholder="e.g. after gym, morning weigh-in")
        ok = st.form_submit_button("💾  Save Entry")

    if ok:
        ds = d.strftime("%Y-%m-%d")
        existing_dates = [x["date"] for x in state["weights"]]
        is_update = ds in existing_dates

        # FIX: rebuild list without the old entry for that date, then append
        state["weights"] = [x for x in state["weights"] if x["date"] != ds]
        state["weights"].append({"date": ds, "weight": float(w), "note": n.strip()})
        state = save_data(state)  # save_data also updates st.session_state.app_state

        if is_update:
            st.success(f"✅ Updated {w} kg for {ds}.")
        else:
            st.success(f"✅ Saved {w} kg on {ds}.")
        st.rerun()

    st.markdown(
        '<div class="small-note">💡 Saving an entry for a date that already exists will overwrite it.</div>',
        unsafe_allow_html=True,
    )

    # ── Delete section ────────────────────────────────────────────────────────
    if state["weights"]:
        st.markdown('<hr><div class="sec-lbl">Delete Entry</div>', unsafe_allow_html=True)
        df_del = pd.DataFrame(state["weights"]).sort_values("date", ascending=False)

        # FIX: use index-based selection so entries with " — " in notes don't break parsing
        opts = {
            f"{r['date']} — {r['weight']} kg" + (f"  ({r['note']})" if r.get("note") else ""): r["date"]
            for _, r in df_del.iterrows()
        }
        to_del_label = st.selectbox("Select entry to delete", list(opts.keys()), label_visibility="collapsed")

        if st.button("🗑️  Delete selected"):
            del_date = opts[to_del_label]  # safe: key lookup, not string split
            state["weights"] = [x for x in state["weights"] if x["date"] != del_date]
            state = save_data(state)
            st.success(f"Deleted entry for {del_date}.")
            st.rerun()

    st.markdown('</div>', unsafe_allow_html=True)


# ── Settings ───────────────────────────────────────────────────────────────────
elif page == "⚙️ Settings":
    st.markdown('<div class="inner-page">', unsafe_allow_html=True)
    st.markdown('<div class="sec-lbl">Goals & Profile</div>', unsafe_allow_html=True)

    with st.form("set_form"):
        gw = st.number_input(
            "Goal weight (kg)",
            min_value=20.0, max_value=300.0,
            value=float(state["goal_weight"]) if state["goal_weight"] else 70.0,
            step=0.5, format="%.1f",
        )
        ht = st.number_input(
            "Height (cm)",
            min_value=100, max_value=250,
            value=int(state["height_cm"]) if state["height_cm"] else 170,
            step=1,
        )
        sv = st.form_submit_button("💾  Save Settings")

    if sv:
        state["goal_weight"] = round(float(gw), 1)
        state["height_cm"] = int(ht)
        state = save_data(state)
        st.success("✅ Settings saved!")

    # ── Import / Export ────────────────────────────────────────────────────────
    st.markdown('<hr><div class="sec-lbl">Import / Export</div>', unsafe_allow_html=True)

    export_col, import_col = st.columns(2)

    with export_col:
        st.download_button(
            "⬇️ Export data",
            data=export_json_bytes(state),
            file_name="weight_tracker_export.json",
            mime="application/json",
            use_container_width=True,
        )
        st.markdown(
            '<div class="small-note">Downloads your full history as JSON.</div>',
            unsafe_allow_html=True,
        )

    with import_col:
        uploaded = st.file_uploader("Import JSON", type=["json"], label_visibility="collapsed")
        if uploaded is not None:
            st.markdown(
                f'<div class="small-note">📄 {uploaded.name} — ready to import</div>',
                unsafe_allow_html=True,
            )
            if st.button("⬆️ Import and replace", use_container_width=True):
                try:
                    imported_state = import_json_bytes(uploaded.getvalue())
                    state = save_data(imported_state)
                    st.success(f"✅ Imported {len(imported_state['weights'])} entries!")
                    st.rerun()
                except Exception as e:
                    st.error(f"Import failed: {e}. Make sure this is a file exported by WeightTracker.")

    # ── Danger zone ────────────────────────────────────────────────────────────
    st.markdown('<hr><div class="sec-lbl">Danger Zone</div>', unsafe_allow_html=True)
    with st.expander("🗑️ Delete all data"):
        st.warning("This will permanently delete all weight entries and reset all settings. This cannot be undone.")
        confirm = st.text_input("Type DELETE to confirm", key="confirm_delete")
        if st.button("Delete everything", key="nuke_btn"):
            if confirm.strip().upper() == "DELETE":
                state = save_data(default_state())
                st.success("All data deleted.")
                st.rerun()
            else:
                st.error("Type DELETE (all caps) to confirm.")

    # ── Meta info ─────────────────────────────────────────────────────────────
    st.markdown('<hr>', unsafe_allow_html=True)
    saved_text = state.get("last_saved_at") or "Not saved yet"
    backup_text = state.get("last_backup_at") or "Not created yet"
    entry_count = len(state.get("weights", []))
    st.markdown(
        f'<div class="small-note">📁 Data file: <code>{os.path.abspath(DATA_FILE)}</code></div>'
        f'<div class="small-note">💾 Last saved: {saved_text}</div>'
        f'<div class="small-note">🔒 Last auto-backup: {backup_text}</div>'
        f'<div class="small-note">📊 Total entries: {entry_count}</div>'
        f'<div class="small-note">⏱ Auto-backup every {AUTO_BACKUP_HOURS} h of use.</div>',
        unsafe_allow_html=True,
    )

    st.markdown('</div>', unsafe_allow_html=True)
