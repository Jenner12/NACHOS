package nachos.vm;

public class PageKey{
    private int pid;
    private int vpn;
    
    public PageKey(int p, int v){
        this.pid = p;
        this.vpn = v;
    }

    public int getPid(){return this.pid;}
    public int getVpn(){return this.vpn;}

    public int hashCode(){
        return new String(this.pid + "-" + this.vpn).hashCode();
    }

    public boolean equals(Object key){
        if (key instanceof PageKey){
            PageKey k = (PageKey) key;
            return (this.pid == k.getPid()) && (this.vpn == k.getVpn()) ;
        }
        else return false;
    }
}