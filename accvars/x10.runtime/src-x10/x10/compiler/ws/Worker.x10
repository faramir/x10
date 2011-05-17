package x10.compiler.ws;

import x10.compiler.Abort;
import x10.compiler.Ifdef;
import x10.compiler.NoReturn;

import x10.lang.Lock;

import x10.util.Random;

public final class Worker {
    public val workers:Rail[Worker];
    private val random:Random;

    public val deque = new Deque();
    public var fifo:Deque = deque; // hack to avoid stealing from null fifo
    public val lock = new Lock();

    public var throwable:Throwable = null;
    
    public def this(i:Int, workers:Rail[Worker]) {
        random = new Random(i + (i << 8) + (i << 16) + (i << 24));
        this.workers = workers;
    }

    public def migrate() {
        var k:RegularFrame;
        lock.lock();
        while (null != (k = Frame.cast[Object,RegularFrame](deque.steal()))) {
            @Ifdef("__CPP__") {
                k = k.remap();
            }
            Runtime.atomicMonitor.lock(); k.ff.asyncs++; Runtime.atomicMonitor.unlock();
            fifo.push(k);
        }
        lock.unlock();
    }

    public def run() {
        try {
            while (true) {
                val k = find();
                if (null == k) return;
                try {
                    unroll(Frame.cast[Object,Frame](k));
                } catch (Abort) {}
            }
        } catch (t:Throwable) {
            Runtime.println("Uncaught exception at place " + here + " in WS worker: " + t);
            t.printStackTrace();
        }
    }

    public def find():Object {
        var k:Object;
        //1) cur thread fifo
        k = fifo.steal();
        while (null == k) {
            if (Runtime.wsEnded()) return null;
            //2) other thread fifo
            val rand = random.nextInt(Runtime.NTHREADS);
            k = workers(rand).fifo.steal();
            if (null != k) break;
            //3) other thread deque
            if (workers(rand).lock.tryLock()) {
                k = workers(rand).deque.steal();
                if (null != k) {
                    var r:RegularFrame = Frame.cast[Object,RegularFrame](k);
                    @Ifdef("__CPP__") {
                        r = r.remap();
                        k = r;
                    }
                    Runtime.atomicMonitor.lock(); r.ff.asyncs++; Runtime.atomicMonitor.unlock();
                }
                workers(rand).lock.unlock();
            }
            if (null != k) break;
            //4) remote activity
            Runtime.wsProcessEvents();
            k = fifo.steal();
        }
        return k;
    }

    @NoReturn public def unroll(var frame:Frame) {
        var up:Frame;
        while (true) {
            frame.wrapResume(this);
            up = frame.up;
            up.wrapBack(this, frame);
            Runtime.deallocObject(frame);
            frame = up;
        }
    }

    public static def runAsyncAt(place:Place, frame:RegularFrame){
        val body = ()=> @x10.compiler.RemoteInvocation { Runtime.wsFIFO().push(frame); };
        Runtime.wsRunAsync(place.id, body);
    }

    @NoReturn static public def runAt(place:Place, frame:RegularFrame){
        val body = ()=> @x10.compiler.RemoteInvocation { Runtime.wsFIFO().push(frame); };
        Runtime.wsRunAsync(place.id, body);
        throw Abort.ABORT;
    }

    public static def stop(){
        val body = ()=> @x10.compiler.RemoteInvocation { Runtime.wsEnd(); };
        for (var i:Int = 1; i<Place.MAX_PLACES; i++) {
            Runtime.wsRunCommand(i, body);
        }
        Runtime.dealloc(body);
        Runtime.wsEnd();
    }

    public static def startHere() {
        Runtime.wsInit();
        val workers = new Rail[Worker](Runtime.NTHREADS);
        for (var i:Int = 0; i<Runtime.NTHREADS; i++) {
            workers(i) = new Worker(i, workers);
        }
        workers(0).fifo = Runtime.wsFIFO();
        for(var i:Int = 1; i<Runtime.NTHREADS; i++) {
            val worker = workers(i);
            async {
                worker.fifo = Runtime.wsFIFO();
                worker.run();
            }
        }
        return workers(0);
    }

    public static def start() {
        val worker = startHere(); // init place 0 first
        for (var i:Int = 1; i<Place.MAX_PLACES; i++) { // init place >0
            val p = Place.place(i);
            async at(p) startHere().run();
        }
        return worker;
    }

    public static def main(frame:MainFrame) {
        val worker = start();
        val ff = frame.ff;
        try {
            frame.fast(worker); // run main activity
        } catch (t:Abort) {
            worker.run(); // join the pool
        } catch (t:Throwable) {
            ff.caught(t); // main terminated abnormally
        } finally {
            stop();
        }
        ff.check();
    }

    public def rethrow() {
        if (null != throwable) {
            val t = throwable;
            throwable = null;
            throw t;
        }
    }
}