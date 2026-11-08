# Eigener Soundtrack für den Bambus-Client-Trailer 3.3 (d-Moll, 150 BPM, 84 s) – dunkel, mit Beat und Drops
import numpy as np, sys
from scipy.signal import butter, sosfilt, fftconvolve, lfilter
from scipy.io import wavfile
SR=44100; LAENGE=84.0; B=0.4; BAR=1.6
N=int(SR*LAENGE); rng=np.random.default_rng(23)
def t_(d): return np.arange(int(SR*d))/SR
def hz(n): return 440*2**((n-69)/12)
def lp(x,f,o=2): return sosfilt(butter(o,min(f,20000),'low',fs=SR,output='sos'),x)
def hp(x,f,o=2): return sosfilt(butter(o,f,'high',fs=SR,output='sos'),x)
def bp(x,a,b,o=2): return sosfilt(butter(o,[a,min(b,20000)],'band',fs=SR,output='sos'),x)
class Spur:
    def __init__(s): s.L=np.zeros(N); s.R=np.zeros(N)
    def add(s,sig,start,gain=1.0,pan=0.0):
        i=int(start*SR)
        if i>=N or i<0: return
        j=min(N,i+len(sig)); x=sig[:j-i]*gain
        s.L[i:j]+=x*(1-max(0,pan)); s.R[i:j]+=x*(1+min(0,pan))
drums=Spur(); orch=Spur(); fx=Spur(); hall=Spur()
def saw(f,d,det=0.0,ph=0.0): t=t_(d); return 2*((t*f*(1+det)+ph)%1)-1
def ens(f,d,n=6,spread=0.008): return sum(saw(f,d,spread*(k-(n-1)/2)/((n-1)/2),rng.random()) for k in range(n))/n
def huelle(d,a,r):
    t=t_(d); return np.minimum(1,t/max(a,1e-4))*np.minimum(1,np.maximum(0,d-t)/max(r,1e-4))
# ------------------------------------------------------------ Instrumente
def streicher(noten,d,hell=2200,a=0.6):
    t=t_(d); x=sum(ens(hz(n),d)*(1+0.004*np.sin(2*np.pi*5.5*t)) for n in noten)/len(noten)
    return lp(x,hell)*huelle(d,a,0.5)
def horn(n,d,a=0.12):
    t=t_(d); x=ens(hz(n),d,4,0.004)+0.5*ens(hz(n-12),d,3,0.003)
    f=lp(x,1300)*0.7+lp(x,700)*0.3
    return np.tanh(f*huelle(d,a,0.25)*1.6)*(1+0.03*np.sin(2*np.pi*5*t))
def floete(n,d):
    t=t_(d); vib=1+0.006*np.sin(2*np.pi*5.2*t)*np.minimum(1,t/0.3)
    ph=2*np.pi*np.cumsum(hz(n)*vib)/SR
    x=np.sin(ph)+0.15*np.sin(2*ph)+0.04*bp(rng.standard_normal(len(t)),2000,6000)
    return x*huelle(d,0.06,0.15)
def pizz(n,d=0.35):
    t=t_(d); x=saw(hz(n),d)*0.6+np.sin(2*np.pi*hz(n)*t)
    return lp(x,2500)*np.exp(-t*11)
def glocke(n,d=1.6):
    t=t_(d); f=hz(n)
    return (np.sin(2*np.pi*f*t)+0.4*np.sin(2*np.pi*f*2.76*t)*np.exp(-t*3)+0.2*np.sin(2*np.pi*f*5.4*t)*np.exp(-t*6))*np.exp(-t*2.2)
def chor(noten,d):
    t=t_(d); x=sum(ens(hz(n),d,5,0.01) for n in noten)/len(noten)
    v=bp(x,450,1000)+0.8*bp(x,1000,1500)+0.3*bp(x,2400,3000)
    return v*huelle(d,0.7,0.6)*(1+0.07*np.sin(2*np.pi*5*t))
def pauke(f0=73.4,stark=1.0):
    t=t_(1.6); f=f0*(1+0.15*np.exp(-t*20)); ph=2*np.pi*np.cumsum(f)/SR
    return (np.sin(ph)*np.exp(-t*2.2)+0.25*lp(rng.standard_normal(len(t)),500)*np.exp(-t*18))*stark
def kick(st=1.0):
    t=t_(0.55); f=44+110*np.exp(-t*28); ph=2*np.pi*np.cumsum(f)/SR
    return np.tanh(np.sin(ph)*np.exp(-t*6)*1.5*st)
def snare():
    t=t_(0.4); return bp(rng.standard_normal(len(t)),1200,8000)*np.exp(-t*12)*1.1+0.5*np.sin(2*np.pi*190*t)*np.exp(-t*20)
def taiko(f0=70):
    t=t_(1.0); f=f0+f0*0.6*np.exp(-t*14); ph=2*np.pi*np.cumsum(f)/SR
    return np.sin(ph)*np.exp(-t*4)+0.2*bp(rng.standard_normal(len(t)),150,1200)*np.exp(-t*14)
def hat(): t=t_(0.06); return hp(rng.standard_normal(len(t)),8000)*np.exp(-t*60)
def crash(): t=t_(3.0); return hp(rng.standard_normal(len(t)),4000)*np.exp(-t*1.4)*0.6
def einschlag():
    t=t_(4.0); f=28+90*np.exp(-t*3); ph=2*np.pi*np.cumsum(f)/SR
    out=np.tanh((np.sin(ph)*np.exp(-t*0.9)*1.3+lp(rng.standard_normal(len(t)),900)*np.exp(-t*5)*1.5)*1.4)
    c=crash(); out[:len(c)]+=c*0.8; return out
def riser(d,top=9000):
    t=t_(d); n=rng.standard_normal(len(t)); out=np.zeros(len(t)); seg=1024
    for k in range(0,len(t),seg):
        f=200+top*(k/len(t))**2; out[k:k+seg]=bp(n[k:k+seg],max(60,f*0.6),f*1.5)
    return out*(t/d)**2.2
def rueckwaerts(d=1.2): c=crash()[:int(d*SR)][::-1]; return c*np.linspace(0,1,len(c))**2
def whoosh(d=0.9):
    t=t_(d); n=rng.standard_normal(len(t)); out=np.zeros(len(t)); seg=1024
    for k in range(0,len(t),seg):
        f=400+7000*(k/len(t)); out[k:k+seg]=bp(n[k:k+seg],f*0.6,f*1.5)
    return out*np.sin(np.pi*t/d)**2
def zap(d=0.12): t=t_(d); return bp(rng.standard_normal(len(t)),1500,6000)*np.exp(-t*30)*(np.sign(np.sin(2*np.pi*60*t))*0.5+0.5)

_KL={}
def klavier(n,d=2.0,vel=0.8):
    key=(n,round(d,2),round(vel,2))
    if key in _KL: return _KL[key]
    t=t_(d); f=hz(n); x=np.zeros(len(t)); Bk=0.00035
    for k in range(1,11):
        fk=f*k*np.sqrt(1+Bk*k*k)
        if fk>16000: break
        amp=(1/k**1.15)*(0.6+0.4*vel)**(k*0.35)
        dec=1.1+f/700+k*0.55
        for det in (-0.0009,0.0,0.0011):
            x+=amp*np.sin(2*np.pi*fk*(1+det)*t+rng.random()*6.28)*np.exp(-t*dec)/3
    ham=lp(rng.standard_normal(len(t)),2500+4000*vel)*np.exp(-t*90)*0.25*vel
    out=(x+ham)*np.minimum(1,t/0.002)*np.minimum(1,np.maximum(0,d-t)/0.08)*vel
    _KL[key]=out; return out
def akkord_kl(noten,start,d=2.0,vel=0.8,gain=0.3,spur=None,arp=0.0):
    for i,n in enumerate(sorted(noten)):
        x=klavier(n,d,vel); pan=-0.45+0.9*(n-36)/60
        orch.add(x,start+i*arp,gain,pan); hall.add(x,start+i*arp,gain*0.55)
def blech(noten,d=0.6):
    x=sum(horn(n,d,0.02) for n in noten)/len(noten); return np.tanh(x*2.2)
def lauf(start,von,anzahl,schritt=0.0625,gain=0.12):   # Streicher-Lauf nach oben (D-Dur-Tonleiter)
    skala=[0,2,4,5,7,9,11]
    for i in range(anzahl):
        n=von+12*(i//7)+skala[i%7]; s=ens(hz(n),0.12,3,0.006)*np.exp(-t_(0.12)*8)
        orch.add(lp(s,4000),start+i*schritt,gain*(0.5+0.5*i/anzahl)); hall.add(lp(s,4000),start+i*schritt,gain*0.4)
# ------------------------------------------------------------ Harmonie (D – A – Hm – G)
D_,A_,Hm,G_=[62,66,69],[61,64,69],[59,62,66],[59,62,67]
EB_=[38,33,35,31]
ACC=[D_,A_,Hm,G_]; BASS=[50,45,47,43]
# Eigene Melodie: (Achtel im 4-Takt-Block, Note, Länge in Achteln)
MEL=[(0,66,2),(2,69,2),(4,74,3),(7,73,1),(8,76,4),(12,73,2),(14,69,2),
     (16,74,2),(18,71,1),(19,73,1),(20,74,2),(22,78,2),(24,79,3),(27,78,1),(28,76,4)]
duck=np.ones(N)
def kick_bei(t0,st=1.0):
    drums.add(kick(st),t0,0.9); i=int(t0*SR); n=int(0.3*SR); j=min(N,i+n)
    if i<N: duck[i:j]=np.minimum(duck[i:j],0.45+0.55*(np.arange(j-i)/n))

def t_clap():
    t=t_(0.3); n=bp(rng.standard_normal(len(t)),900,6000); env=np.zeros(len(t))
    for v in (0.0,0.011,0.022): env+=np.exp(-np.maximum(0,t-v)*60)*(t>=v)
    return n*(env*0.5+np.exp(-t*14))*0.9
def t_ohat(): t=t_(0.2); return hp(rng.standard_normal(len(t)),7000)*np.exp(-t*15)
def t_shaker(): t=t_(0.08); return bp(rng.standard_normal(len(t)),5000,12000)*np.exp(-t*45)
def t_bass16(n,d=0.125):
    t=t_(d); x=saw(hz(n),d)+0.7*np.sign(np.sin(2*np.pi*hz(n)*t))
    return np.tanh((lp(x,1200)*np.exp(-t*30)+lp(x,650)*(1-np.exp(-t*30)))*np.exp(-t*7)*1.6)
def t_pluck(noten,d=0.16,cut=4800):
    x=sum(ens(hz(n),d,5,0.016) for n in noten)/len(noten); return lp(x,cut)*np.exp(-t_(d)*13)
def treibende_drums(t,stark=1.0,clap_an=True):
    for sch in range(4):
        kick_bei(t+sch*B,1.15*stark)
        if clap_an and sch in (1,3): drums.add(t_clap(),t+sch*B,0.5*stark); hall.add(t_clap(),t+sch*B,0.12)
        drums.add(t_ohat(),t+sch*B+B/2,0.17*stark,0.2)
        for e in range(4): drums.add(hat(),t+sch*B+e*B/4,(0.09 if e%2 else 0.05)*stark,(-0.3 if e%2 else 0.3))
        drums.add(t_shaker(),t+sch*B+B*0.75,0.07*stark,-0.4)
def treibender_bass(t,bs,gain=0.3):
    for e in range(16):
        n=bs if e%4!=2 else bs+12
        orch.add(t_bass16(n),t+e*B/4,gain*(1.0 if e%4==0 else 0.75))
def block(von,bis,REF=16.0,gitarre=False,melodie='floete',drums_art='mittel',pizzicato=True,ostinato=False,horn_mel=False,chorus=False,klav='arp',blechstoss=False):
    t=von
    while t<bis-1e-6:
        k=int(round((t-REF)/BAR))%4; ac=ACC[k]; bs=BASS[k]
        orch.add(streicher([n-12 for n in ac]+[ac[0]],BAR+0.05,1800),t,0.30); hall.add(streicher(ac,BAR+0.05),t,0.25)
        orch.add(streicher([bs],BAR+0.05,500,0.05),t,0.35)
        if chorus: hall.add(chor(ac,BAR),t,0.45); orch.add(chor(ac,BAR),t,0.12)
        if klav:
            if klav=='arp':   # schnelle Sechzehntel-Arpeggios
                muster=[ac[0],ac[1],ac[2],ac[1]+12,ac[2]+12,ac[1]+12,ac[2],ac[1]]
                for e in range(16): akkord_kl([muster[e%8]],t+e*B/4,0.6,0.5+0.15*(e%4==0),0.17)
            else:            # wuchtige Oktav-Akkorde im Drop
                for e in range(8):
                    v=0.95 if e%2==0 else 0.75
                    akkord_kl([bs-12,bs]+ ([n+12 for n in ac] if e%2==0 else ac),t+e*B/2,0.6,v,0.15)
        if pizzicato:
            muster=[ac[0],ac[1],ac[2],ac[1]+12,ac[2],ac[1],ac[0]+12,ac[2]]
            for e,n in enumerate(muster): orch.add(pizz(n),t+e*B/2,0.16,(-0.35 if e%2 else 0.35)); hall.add(pizz(n),t+e*B/2,0.06)
        if ostinato:
            for e in range(16): s=lp(ens(hz(ac[0]-12+(12 if e%4==2 else 0)),0.11,4,0.006),2600)*np.exp(-t_(0.11)*10); orch.add(s,t+e*B/4,0.22)
        if melodie:
            blk=int(round((t-REF)/BAR))%4
            for (ach,n,l) in MEL:
                if blk*8<=ach<(blk+1)*8:
                    st=t+(ach-blk*8)*B/2; d=l*B/2+0.15
                    if melodie=='floete': s=floete(n,d); orch.add(s,st,0.22,0.1); hall.add(s,st,0.3)
                    if melodie=='klavier': akkord_kl([n,n+12] if horn_mel else [n],st,d+0.6,0.95,0.30)
                    if horn_mel: h=horn(n-12,d); orch.add(h,st,0.3,-0.1); hall.add(h,st,0.25)
        if drums_art=='treibend':
            treibende_drums(t,1.0); treibender_bass(t,bs-12,0.26)
            for sch in range(4):
                x=t_pluck([n+12 for n in ac],0.16); orch.add(x,t+sch*B+B/2,0.2,(0.3 if sch%2 else -0.3)); hall.add(x,t+sch*B+B/2,0.12)
        if drums_art in ('mittel','voll'):
            for sch in range(4):
                kick_bei(t+sch*B,1.0 if drums_art=='mittel' else 1.25)
                if sch in (1,3): drums.add(snare(),t+sch*B,0.45 if drums_art=='mittel' else 0.6); hall.add(snare(),t+sch*B,0.15)
                drums.add(hat(),t+sch*B+B/2,0.14,0.3)
                if drums_art=='voll': drums.add(hat(),t+sch*B+B/4,0.08,-0.3); drums.add(taiko(65 if sch%2 else 80),t+sch*B+B/4*3,0.35)
            if drums_art=='voll': drums.add(crash(),t,0.22)
        if blechstoss:
            for x_ in (0.0,0.6,1.2): orch.add(blech([n-12 for n in ac]+[ac[0]],0.45),t+x_,0.28); hall.add(blech([n-12 for n in ac],0.45),t+x_,0.2)
            drums.add(taiko(55),t,0.6); drums.add(taiko(55),t+0.6,0.5); drums.add(taiko(70),t+1.2,0.55)
        if gitarre: gitarren_chugs(t,bs,0.2 if gitarre==True else gitarre)
        t+=BAR

def ssaw(noten,d,cut=6000,det=0.02,stimmen=7):
    x=sum(ens(hz(n),d,stimmen,det) for n in noten)/len(noten); return lp(x,cut)
def pluck(noten,d=0.22,cut=5500):
    return ssaw(noten,d,cut)*np.exp(-t_(d)*11)
def lead_ss(n,d):
    t=t_(d); x=ens(hz(n),d,7,0.022)+0.5*ens(hz(n+12),d,5,0.015)
    return lp(x,7500)*huelle(d,0.01,0.12)*(1+0.02*np.sin(2*np.pi*6*t))
def wobble(n,d,rate=4.0):
    t=t_(d); x=saw(hz(n),d)+0.8*np.sign(np.sin(2*np.pi*hz(n)*t))+0.6*np.sin(2*np.pi*hz(n-12)*t)
    out=np.zeros(len(t)); seg=256
    for k in range(0,len(t),seg):
        tt=k/SR; f=180+2200*(0.5+0.5*np.sin(2*np.pi*rate*tt-np.pi/2))
        out[k:k+seg]=lp(x[k:k+seg],f,2)
    return np.tanh(out*1.8)*huelle(d,0.005,0.02)
def clap():
    t=t_(0.3); n=bp(rng.standard_normal(len(t)),900,6000); env=np.zeros(len(t))
    for v in (0.0,0.011,0.022): env+=np.exp(-np.maximum(0,t-v)*60)*(t>=v)
    return n*(env*0.5+np.exp(-t*14))*0.9
def offenhat(): t=t_(0.22); return hp(rng.standard_normal(len(t)),7000)*np.exp(-t*14)
def sweep(noten,d,von=250,bis=9000):
    x=ssaw(noten,d,20000); out=np.zeros(len(x)); seg=512
    for k in range(0,len(x),seg):
        p=k/len(x); out[k:k+seg]=lp(x[k:k+seg],von*(bis/von)**p)
    return out*np.minimum(1,t_(d)/0.3)

# ============================================================ NEUE INSTRUMENTE
def gitarre(noten,d,mute=True):
    """Verzerrte Powerchord-Gitarre (palm-muted = kurz und dumpf)."""
    t=t_(d); x=sum(saw(hz(n),d,0.003*k,rng.random()) for k,n in enumerate(noten))/len(noten)
    x=x+0.6*sum(np.sign(np.sin(2*np.pi*hz(n)*1.002*t)) for n in noten)/len(noten)
    x=np.tanh(lp(x,3200 if not mute else 1800)*6)
    x=bp(x,90,5000)
    env=np.exp(-t*(14 if mute else 1.6))*np.minimum(1,t/0.003)
    return x*env*np.minimum(1,np.maximum(0,d-t)/0.02)
def gitarren_chugs(t0,bs,gain=0.2):
    """Achtel-Chugs mit Akzenten – bringt den Schwung."""
    muster=[1,0,1,1,0,1,1,1, 1,0,1,1,0,1,1,1]
    for e in range(16):
        if not muster[e]: continue
        n=bs if bs>=36 else bs+12
        x=gitarre([n,n+7],0.1); akz=1.0 if e%4==0 else 0.7
        orch.add(x,t0+e*B/4,gain*akz,-0.55); orch.add(x,t0+e*B/4+0.012,gain*akz*0.9,0.55)
def braam(noten,d=1.5,st=1.0):
    """Tiefer, verzerrter Blechbläser-Stoß (Trailer-BRAAM)."""
    t=t_(d); x=sum(ens(hz(n),d,6,0.012) for n in noten)/len(noten)
    f=np.zeros(len(t)); seg=512
    for k in range(0,len(t),seg):
        tt=k/SR; cut=300+2600*np.exp(-tt*2.5)*min(1,tt/0.06+0.2); f[k:k+seg]=lp(x[k:k+seg],cut)
    return np.tanh(f*3.5*st)*huelle(d,0.03,0.6)
def stoss(noten,d=0.35):
    """Kurzer Orchester-Hit: Blech + Streicher."""
    x=blech(noten,d)+0.6*lp(sum(ens(hz(n+12),d,5,0.008) for n in noten)/len(noten),5000)*np.exp(-t_(d)*6)
    return x*huelle(d,0.005,0.08)
def holzblock(): t=t_(0.06); return np.sin(2*np.pi*1800*t)*np.exp(-t*90)+0.3*bp(rng.standard_normal(len(t)),2500,6000)*np.exp(-t*120)
def tom(f0=110): t=t_(0.5); f=f0*(1+0.5*np.exp(-t*25)); ph=2*np.pi*np.cumsum(f)/SR; return np.sin(ph)*np.exp(-t*7)+0.15*bp(rng.standard_normal(len(t)),200,3000)*np.exp(-t*25)
def ride(): t=t_(0.5); return hp(rng.standard_normal(len(t)),6000)*np.exp(-t*6)*0.5+0.15*np.sin(2*np.pi*5200*t)*np.exp(-t*5)
def sub(f=36.7,d=1.4): t=t_(d); return np.sin(2*np.pi*f*t)*huelle(d,0.02,0.5)
def taiko_takt(t0,st=1.0):
    for x,f,gn in ((0,60,0.6),(B*1.5,70,0.4),(B*2,60,0.55),(B*3,80,0.45),(B*3.5,70,0.4)):
        drums.add(taiko(f),t0+x,gn*st,(rng.random()-.5)*0.4); hall.add(taiko(f),t0+x,0.15*st)
def lead_takt(t,ref,gain=0.22,okt=12):
    blk=int(round((t-ref)/BAR))%4
    for (ach,n,l) in MEL:
        if blk*8<=ach<(blk+1)*8:
            st2=t+(ach-blk*8)*B/2; x=lead_ss(n+okt,l*B/2+0.1)
            orch.add(x,st2,gain,-0.1); orch.add(x,st2+0.3,gain*0.3,0.4); hall.add(x,st2,gain*1.1)


# ============================================================ DUNKEL & BEDROHLICH (d-Moll, tiefe Blechbläser, große Drops)
# Alle Fis werden zu F und alle H zu B → alles, was vorher in D-Dur stand, klingt jetzt in d-Moll.
# Das Cis (A-Dur-Akkord) bleibt für die spannungsgeladene Dominante.
def hz(n):
    k=int(round(n)) % 12
    if k in (6,11): n-=1
    return 440*2**((n-69)/12)
Bb_,Gm_=[58,62,65],[55,58,62]
ACC=[D_,Bb_,Gm_,A_]; BASS=[50,46,43,45]; EB_=[38,34,31,33]
# Eigenes dunkles Thema: Grundton – kleine Terz – Quinte, dann über B und g-Moll zur Dominante A
MEL=[(0,62,2),(2,62,1),(3,65,1),(4,69,4),
     (8,70,3),(11,69,1),(12,65,4),
     (16,67,2),(18,70,2),(20,74,4),
     (24,73,4),(28,69,4)]
_braam_hell=braam
def braam(noten,d=1.5,st=1.0):       # schwerer: tiefe Oktave + Sub darunter
    tief=[n-12 for n in noten if n-12>=24]
    x=_braam_hell(list(noten)+tief,d,st*1.15)
    t=t_(d); s=np.sin(2*np.pi*hz(min(noten)-12 if min(noten)>=36 else min(noten))*t)*huelle(d,0.02,0.8)
    return x*0.85+0.5*s
def lead_takt(t,ref,gain=0.22,okt=12):            # Thema tief in Posaunen/Hörnern, Streicher eine Oktave darüber
    blk=int(round((t-ref)/BAR))%4
    for (ach,n,l) in MEL:
        if blk*8<=ach<(blk+1)*8:
            st2=t+(ach-blk*8)*B/2; d=l*B/2+0.12
            x=lead_ss(n-12+okt-12,d); orch.add(x,st2,gain*1.3,-0.15); hall.add(x,st2,gain*0.8)
            y=blech([n-24+okt-12+12],d); orch.add(y,st2,gain*0.7,0.15)
def spannung(d=2.0):                  # steigender, sirrender Ton vor einem Drop
    t=t_(d); f=hz(38)*2**(3*(t/d)**1.6); ph=2*np.pi*np.cumsum(f)/SR
    x=(np.sign(np.sin(ph))*0.3+np.sin(ph*1.003))*(t/d)**2.2
    n=len(x); h=n//2
    y=np.concatenate([lp(x[:h],900),lp(x[h:],4500)])   # wird heller
    return y*0.5+riser(d,12000)[:n]*0.5
def unterdrop(d=2.4):                 # Sub, der nach unten fällt
    t=t_(d); f=70*np.exp(-t*1.2)+28; ph=2*np.pi*np.cumsum(f)/SR
    return np.tanh(np.sin(ph)*2.2)*np.exp(-t*1.1)
def dunkler_drop(tt,st=1.0,mit_spannung=True):
    if mit_spannung: fx.add(spannung(2.0),tt-2.0,0.55*st)
    x=braam([26,38,45,50,53],2.6,1.5); orch.add(x,tt,0.7*st,-0.12); orch.add(x,tt+0.02,0.65*st,0.12); hall.add(x,tt,0.45*st)
    drums.add(einschlag(),tt,1.0*st); drums.add(unterdrop(),tt,0.9*st); drums.add(pauke(55.0,1.4),tt,0.8*st)
    drums.add(crash(),tt,0.35*st); hall.add(chor([62,65,69],2.4),tt,0.25*st)


# Mehr Beat: härterer Kick mit Distortion-Klick und Reese-Bass unter den Sechzehnteln
_kick_alt=kick
def kick(st=1.0):
    k=_kick_alt(st); t=t_(len(k)/SR)
    return np.tanh(k*1.6+0.25*np.sin(2*np.pi*48*t)*np.exp(-t*5))
_bass16_alt=t_bass16
def t_bass16(n,d=0.125):
    t=t_(d); x=saw(hz(n),d,0.0)+saw(hz(n),d,0.012)+saw(hz(n),d,-0.01)
    return np.tanh(lp(x,700)*np.exp(-t*7)*1.6)*0.7+_bass16_alt(n,d)*0.5
# ============================================================ INTRO 0–16 (epischer)
# Dunkler Grund: Drone + Sub, Uhr-Ticken treibt von Anfang an
orch.add(lp(ens(hz(26),16,7,0.01),220)*huelle(16,1.5,1.0),0,0.35)
for k in range(40):                                   # Holzblock-Ticken in Achteln, ab 3.6 in Sechzehnteln
    tt=0.4+k*0.2
    if tt<3.6: drums.add(holzblock(),tt,0.10+0.04*(k%2==0),(0.4 if k%2 else -0.4))
for k in range(84):
    tt=3.6+k*0.1
    if tt<12: drums.add(holzblock(),tt,(0.08 if k%2 else 0.13)+0.04*(tt-3.6)/8.4,(0.4 if k%2 else -0.4))
# Drei BRAAMS (mit Rückwärts-Becken davor)
for i,(tt,ns) in enumerate(((0.4,[26,38,45]),(2.0,[26,38,46]),(3.6,[26,38,45,50]))):
    if tt>0.6: fx.add(rueckwaerts(0.9),tt-0.9,0.45)
    x=braam([n for n in ns],1.5,1.0+0.25*i); orch.add(x,tt,0.55,-0.15); orch.add(x,tt+0.015,0.5,0.15); hall.add(x,tt,0.35)
    drums.add(sub(36.7,1.6),tt,0.7); drums.add(pauke(55.0,1.2),tt,0.7); drums.add(einschlag(),tt,0.35+0.15*i)
# Klavier-Motiv: schnelle Sechzehntel ab 0.4, leise -> lauter
for e in range(32):
    ac=[D_,D_,A_,A_][e//8]; n=[ac[0],ac[1],ac[2],ac[1]+12][e%4]+12
    akkord_kl([n],0.4+e*0.1,0.5,0.45+0.3*e/32,0.12+0.08*e/32)
# 3.6: Synthwave geht auf – Taikos, Spiccato, halber Beat
for tb in np.arange(3.6,12.0-1e-6,BAR):
    k=int(round((tb-3.6)/BAR))%4; ac=ACC[k]
    taiko_takt(tb,0.7+0.4*(tb-3.6)/8.4)
    for e in range(16):
        x=lp(ens(hz(ac[0]-12+(12 if e%4==2 else 0)),0.09,4,0.006),2600)*np.exp(-t_(0.09)*14)
        orch.add(x,tb+e*B/4,0.12+0.08*(tb-3.6)/8.4,(0.3 if e%2 else -0.3))
    orch.add(streicher(ac,BAR+0.05,1200+200*k,0.2),tb,0.22); hall.add(streicher(ac,BAR+0.05,1800),tb,0.2)
    for sch in (0,2): kick_bei(tb+sch*B,0.8)
    gitarren_chugs(tb,BASS[k]-12,0.08+0.08*(tb-3.6)/8.4)
# Text-Slams 4.8 / 5.6 / 6.4: Orchester-Hits
for tt,ac in ((4.8,Hm),(5.6,G_),(6.4,A_)):
    x=stoss([n-12 for n in ac]+[ac[0]],0.5); orch.add(x,tt,0.45); hall.add(x,tt,0.3)
    drums.add(taiko(55),tt,0.8); drums.add(crash(),tt,0.3); drums.add(sub(hz(ac[0]-36),0.7),tt,0.6)
fx.add(riser(0.8,9000),7.2,0.35)
# 8.0–12: Ringe bauen sich auf (8.0/8.8/9.6/10.4) – Blech-Stöße + Chor steigt
for tt,ac in ((8.0,D_),(8.8,G_),(9.6,Hm),(10.4,A_)):
    x=stoss([n-12 for n in ac]+[ac[0],ac[0]+12],0.6); orch.add(x,tt,0.42); hall.add(x,tt,0.32)
    drums.add(taiko(60),tt,0.7); drums.add(einschlag(),tt,0.2)
hall.add(chor(D_,3.2),8.0,0.4); hall.add(chor([n+12 for n in A_],3.2),9.6,0.5); hall.add(chor([n+12 for n in A_]+[81],1.6),10.4,0.5)
for k in range(16):                                   # Four-on-the-floor ab 8.0
    tt=8.0+k*B; kick_bei(tt,0.9+0.4*k/16)
    drums.add(t_ohat(),tt+B/2,0.08+0.1*k/16)
for k in range(6):                                    # Blitze ab 9.6: Zap + Tom auf jedem Schlag
    tt=9.6+k*B; fx.add(zap(0.18),tt,0.3,(rng.random()-.5)); drums.add(tom(90+k*12),tt,0.4)
rz=10.4
for d in [0.2]*4+[0.1]*8:
    if rz<11.95: drums.add(snare(),rz,0.18+0.45*(rz-10.4)/1.6); hall.add(snare(),rz,0.08)
    rz+=d
lauf(11.2,62,16,0.05,0.15); fx.add(riser(3.6,14000),8.4,0.5); fx.add(rueckwaerts(1.0),11.0,0.6)
# 12.0: EINSCHLAG + Fanfare
drums.add(einschlag(),12.0,1.0); hall.add(einschlag(),12.0,0.5); drums.add(sub(36.7,2.0),12.0,0.9)
x=braam([26,38,45,50,57],2.0,1.4); orch.add(x,12.0,0.5); hall.add(x,12.0,0.35)
for tt,ac,d in ((12.0,D_,1.65),(13.6,G_,0.85),(14.4,A_,0.65),(15.0,A_,0.6)):
    orch.add(streicher(ac+[ac[0]+12],d,3200,0.03),tt,0.35); hall.add(chor(ac+[ac[0]+12],d),tt,0.6)
for i in range(6):                                    # Buchstaben B-A-M-B-U-S: Toms + Pauke
    tt=12.4+i*0.2; drums.add(tom(140-i*12),tt,0.55); drums.add(pauke(55.0,0.6),tt,0.35)
drums.add(einschlag(),13.6,0.6); drums.add(crash(),13.6,0.4)      # CLIENT
for (tt,n,d) in ((12.0,74,0.75),(12.8,76,0.35),(13.2,78,0.35),(13.6,79,0.75),(14.4,81,0.55),(15.0,78,0.55)):   # Fanfare
    orch.add(horn(n-12,d,0.03),tt,0.36); hall.add(horn(n-12,d,0.03),tt,0.28)
    orch.add(lead_ss(n,d),tt,0.12)
for (tt,ns) in ((12.0,[38,50,62,66,69,74]),(13.6,[31,43,62,67,71,79]),(14.4,[33,45,61,64,69,81])):
    akkord_kl(ns,tt,1.2,1.0,0.22)
for k in range(9):                                    # treibende Achtel bis zum Drop
    tt=12.0+k*B
    if tt<15.0: kick_bei(tt,1.1); drums.add(t_ohat(),tt+B/2,0.15)
for e in range(30):
    tt=12.0+e*0.1
    if tt<15.0: orch.add(t_bass16(38 if tt<13.6 else (31 if tt<14.4 else 33)),tt,0.22)
gitarren_chugs(12.0,38,0.16); gitarren_chugs(13.6,31,0.16)
fx.add(riser(1.0,15000),14.6,0.5); fx.add(rueckwaerts(0.9),14.7,0.65)
rz=15.0
for d in [0.1]*6: drums.add(snare(),rz,0.4); rz+=d

# ============================================================ DROP 16–22 (Kinetik, ein Wort pro Schlag)
drums.add(einschlag(),16.0,1.0); hall.add(einschlag(),16.0,0.5); drums.add(sub(36.7,1.6),16.0,0.8)
block(16,22,REF=16.0,gitarre=0.22,melodie='klavier',drums_art='treibend',pizzicato=False,ostinato=True,horn_mel=True,chorus=True,klav='hammer',blechstoss=True)
for tb in np.arange(16,22-1e-6,BAR): lead_takt(tb,16.0,0.16)
for k in range(6): drums.add(tom(120),16.0+k*B,0.35)   # Wort-Schläge

# ============================================================ LAUNCHER 22–66
MARKEN={}
for z in open(sys.argv[2]):
    tt,nn=z.split(); MARKEN[nn]=float(tt)+24.4
WECHSEL=[22.0]+[MARKEN[n] for n in ('nav-module','nav-kosmetik','tab-fluegel','tab-huete','erstellen','speichern','nav-mods','nav-start')]
block(22,38,REF=22.0,gitarre=0.12,melodie='klavier',drums_art='treibend',pizzicato=False,klav='arp')
block(38,54,REF=22.0,gitarre=0.16,melodie='klavier',drums_art='treibend',pizzicato=False,horn_mel=True,chorus=True,klav='arp')
block(54,66,REF=22.0,gitarre=0.2,melodie='klavier',drums_art='voll',pizzicato=False,ostinato=True,horn_mel=True,chorus=True,klav='hammer',blechstoss=True)
for tb in np.arange(22,66-1e-6,BAR):
    if tb>=38: taiko_takt(tb,0.45 if tb<54 else 0.7)
    if tb>=38: [drums.add(ride(),tb+k_*B,0.06) for k_ in range(4)]
    if tb>=54: lead_takt(tb,22.0,0.18); treibender_bass(tb,EB_[int(round((tb-22)/BAR))%4],0.2)
for tb in np.arange(16,66-1e-6,BAR):                   # pumpender Sub-Bass auf den Offbeats – Druck von unten
    bs=BASS[int(round((tb-(16.0 if tb<22 else 22.0))/BAR))%4]
    for sch in range(4):
        tt_=t_(0.19); sb=(np.sin(2*np.pi*hz(bs-12)*tt_)+0.6*np.sin(2*np.pi*hz(bs-24)*tt_))*np.minimum(1,tt_/0.006)*np.minimum(1,(0.19-tt_)/0.03)
        drums.add(np.tanh(sb*1.4),tb+sch*B+B/2,0.6)
for n_ in ('polarlicht','glut','phoenix','engel','krone','heiligenschein','eigene-fluegel','fps-aus','fps-an'):   # Akzente bei jeder Auswahl
    tt=MARKEN[n_]; orch.add(glocke(86),tt,0.14); hall.add(glocke(86),tt,0.18); drums.add(tom(150),tt,0.25)
for z in WECHSEL:
    fx.add(whoosh(0.9),z-0.7,0.28); drums.add(crash(),z,0.2); orch.add(stoss([n-12 for n in D_],0.3),z,0.18)
drums.add(einschlag(),38.0,0.4); drums.add(einschlag(),54.0,0.5); drums.add(einschlag(),MARKEN['fps-an'],0.6); fx.add(riser(1.2,12000),MARKEN['fps-an']-1.2,0.35); fx.add(riser(2.4,12000),51.6,0.35)

# ============================================================ INSTALLATION 66–72
drums.add(einschlag(),66.0,0.9); drums.add(sub(36.7,1.6),66.0,0.7)
for k,ac in enumerate([D_,Hm,G_,A_]):
    tt=66+k*BAR
    if tt>=71.6: break
    orch.add(streicher(ac+[ac[0]+12],BAR,2600,0.1),tt,0.4); hall.add(chor(ac,BAR),tt,0.55)
    x=stoss([n-12 for n in ac]+[ac[0]],0.5); orch.add(x,tt,0.35); hall.add(x,tt,0.25)
    akkord_kl([ac[0]-24,ac[0]-12]+ac+[ac[0]+12],tt,1.6,0.95,0.2,arp=0.03)
    taiko_takt(tt,0.9); gitarren_chugs(tt,[38,35,31,33][k],0.2)
for k in range(14):
    tt=66+k*B
    if tt<71.6: kick_bei(tt,1.1); [drums.add(hat(),tt+e*B/4,0.07) for e in range(4)]; drums.add(t_ohat(),tt+B/2,0.15)
rz=70.0
for d in [0.2]*2+[0.1]*4+[0.05]*8:
    if rz<71.68: drums.add(snare(),rz,0.2+0.45*(rz-70)/1.7)
    rz+=d
lauf(70.8,62,16,0.05,0.15); fx.add(riser(1.7,15000),70.0,0.55)
orch.add(sweep([62,66,69,74],1.7,200,9000),70.0,0.35)

# ============================================================ ELEKTRO-OUTRO 72–84
ELEK=[D_,A_,Hm,G_]; EBASS=[38,33,35,31]
drums.add(einschlag(),72.0,1.0); hall.add(einschlag(),72.0,0.5)
def elektro_drop(von,bis,ref,lead=True):
    t=von
    while t<bis-1e-6:
        k=int(round((t-ref)/BAR))%4; ac=ELEK[k]; bs=EBASS[k]
        drums.add(crash(),t,0.25)
        for sch in range(4):
            kick_bei(t+sch*B,1.35)
            if sch in (1,3): drums.add(clap(),t+sch*B,0.55); hall.add(clap(),t+sch*B,0.18)
            drums.add(offenhat(),t+sch*B+B/2,0.2,0.2)
            for e in range(4): drums.add(hat(),t+sch*B+e*B/4,0.08,(-0.3 if e%2 else 0.3))
            st=pluck([n+12 for n in ac]+[ac[0]+24],0.22); orch.add(st,t+sch*B+B/2,0.36,(-0.25 if sch%2 else 0.25)); hall.add(st,t+sch*B+B/2,0.25)
        orch.add(ssaw([n+12 for n in ac],BAR,3500),t,0.22); hall.add(ssaw(ac,BAR,3000),t,0.2)
        for e in range(8): orch.add(wobble(bs,B/2,4.0 if e%2==0 else 8.0),t+e*B/2,0.4)
        gitarren_chugs(t,bs,0.16)
        if lead: lead_takt(t,ref,0.26)
        t+=BAR
elektro_drop(72,76.8,72.0)
for k,ac in enumerate([D_,A_]):                       # 76.8–79.5: Arp-Aufbau
    tt=76.8+k*BAR
    if tt>=79.5: break
    kick_bei(tt,1.0); kick_bei(tt+2*B,1.0)
    hall.add(chor(ac+[ac[0]+12],BAR),tt,0.5); orch.add(ssaw(ac,BAR,1800),tt,0.18)
    for e in range(16):
        if tt+e*0.1>=79.45: break
        n=[ac[0],ac[1],ac[2],ac[1]+12][e%4]+12; cut=900+5000*((k*16+e)/32)
        x=pluck([n],0.16,cut); orch.add(x,tt+e*0.1,0.3,(0.35 if e%2 else -0.35)); hall.add(x,tt+e*0.1,0.15)
rz=78.3
for d in [0.2]*2+[0.1]*4+[0.05]*6:
    if rz<79.45: drums.add(snare(),rz,0.2+0.4*(rz-78.3)/1.1); drums.add(kick(0.7),rz,0.2+0.3*(rz-78.3)/1.1)
    rz+=d
fx.add(riser(2.4,15000),77.1,0.6); fx.add(rueckwaerts(1.0),78.5,0.6)
# 79.5–82.6: FINAL-DROP
drums.add(einschlag(),79.5,1.0); hall.add(einschlag(),79.5,0.5); drums.add(crash(),79.5,0.7)
for sch in range(8):
    tt=79.5+sch*B
    if tt>=82.6: break
    kick_bei(tt,1.4)
    if sch%2: drums.add(clap(),tt,0.6)
    drums.add(offenhat(),tt+B/2,0.2)
    for e in range(2): orch.add(wobble(38,B/2,8.0),tt+e*B/2,0.4)
    orch.add(pluck([n+12 for n in [62,66,69]],0.22),tt+B/2,0.38)
gitarren_chugs(79.5,38,0.2); gitarren_chugs(81.1,38,0.2)
schluss=[50,57,62,66,69,74,78]
orch.add(ssaw(schluss,3.1,5000),79.5,0.35); hall.add(ssaw(schluss,3.1,4000),79.5,0.3); hall.add(chor([62,66,69,74],3.1),79.5,0.6)
for (tt,n,d) in ((79.5,74,0.4),(79.9,78,0.4),(80.3,81,0.8),(81.1,86,1.2)):
    x=lead_ss(n+12,d); orch.add(x,tt,0.3); hall.add(x,tt,0.3)
    x=horn(n-12,d,0.03); orch.add(x,tt,0.25)
akkord_kl([26,38,50,57,62,66,69,74],79.5,3.0,1.0,0.14,arp=0.02)
for x in (82.6,82.85,83.0,83.1,83.35): fx.add(zap(),x,0.3,(rng.random()-.5))
drums.add(einschlag(),83.55,0.6); drums.add(kick(1.4),83.55,0.8); x=braam([26,38,45],1.2,1.3); orch.add(x,83.55,0.4)
# ============================================================ DROPS (dunkel, mit Anlauf)
# Bedrohliches Brummen unter dem Intro: tiefes D + A, langsam pulsierend
t=t_(15.6); drohne=(np.sin(2*np.pi*hz(26)*t)+0.6*ens(hz(38),15.6,4,0.004)+0.35*ens(hz(45),15.6,4,0.004))
drohne=lp(drohne,420)*(0.55+0.45*np.sin(2*np.pi*t/1.6)**2)*np.minimum(1,t/2.5)*np.minimum(1,(15.6-t)/0.4)
orch.add(np.tanh(drohne*1.4),0.0,0.42); hall.add(drohne,0.0,0.12)
for k in range(int(15.6/0.8)): drums.add(kick(0.9),0.0+k*0.8,0.12+0.22*k/19)   # Herzschlag wird lauter
for tt,st,sp in ((16.0,1.0,True),(22.0,1.1,True),(38.0,0.8,False),(54.0,0.85,False),(72.0,1.25,True),(79.5,1.3,True)):
    dunkler_drop(tt,st,sp)

# ============================================================ Mischen
t=t_(2.2); ir=rng.standard_normal((len(t),2))*np.exp(-t*3.2)[:,None]; ir[:,0]=lp(ir[:,0],6500); ir[:,1]=lp(ir[:,1],6500)
ir/=np.sqrt((ir**2).sum(0))[None,:]   # Hall normiert: klar statt verwaschen
hL=fftconvolve(hall.L,ir[:,0])[:N]; hR=fftconvolve(hall.R,ir[:,1])[:N]
pause=np.ones(N)
for a_,e_,gf in ((0.0,3.6,0.7),(3.6,12.0,0.85),(12.0,15.6,0.95),(16.0,22.0,1.1),(22.0,38.0,1.3),(38.0,54.0,1.25),(54.0,66.0,1.3),(66.0,71.72,1.1)): pause[int(a_*SR):int(e_*SR)]=gf
for a_,e_ in ((15.6,16.0),(21.75,22.0),(71.72,72.0),(79.15,79.5)):
    i0,i1=int(a_*SR),int(e_*SR); pause[i0:i1]=0.0; r=int(0.01*SR); pause[i0:i0+r]=np.linspace(1,0,r)
L=(drums.L*0.7+orch.L*duck*1.45+hL*2.2*duck)*pause+fx.L
R=(drums.R*0.7+orch.R*duck*1.45+hR*2.2*duck)*pause+fx.R
st=np.stack([L,R],1)
a0,a1=int(82.6*SR),int(83.5*SR); n=a1-a0
speed=np.linspace(1,0,n)**1.3; pos=a0+np.cumsum(speed)
for ch in range(2):
    st[a0:a1,ch]=np.interp(pos,np.arange(N),st[:,ch])*np.linspace(1,0.2,n)
i2=int(83.55*SR); st[a1:i2]*=0.0
fade=np.ones(N); fn=int(1.6*SR); fade[-fn:]=np.linspace(1,0,fn)**1.2; st=st*fade[:,None]
def kompressor(x,thr_db=-18,ratio=4,att=0.005,rel=0.25):
    pegel=np.max(np.abs(x),axis=1); a_a=np.exp(-1/(att*SR)); a_r=np.exp(-1/(rel*SR))
    blk=256; nb=len(pegel)//blk+1; env=np.zeros(nb); e=0
    for i in range(nb):
        m=pegel[i*blk:(i+1)*blk].max() if i*blk<len(pegel) else 0
        e=m if m>e else e*(a_r**blk); env[i]=e
    env=np.repeat(env,blk)[:len(pegel)]+1e-9; thr=10**(thr_db/20)
    g=np.where(env>thr,(thr/env)**(1-1/ratio),1.0); g=lfilter([1-a_a**8],[1,-(a_a**8)],g)
    return x*g[:,None]
st=st/np.max(np.abs(st)); st=kompressor(st); st=st/np.max(np.abs(st)); st=np.tanh(st*1.5); st=st/np.max(np.abs(st))*0.9
wavfile.write(sys.argv[1],SR,(st*32767).astype(np.int16)); print('ok')
