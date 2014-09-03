package me.confuser.barapi.nms.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.server.v1_7_R4.ChunkCoordinates;
import net.minecraft.server.v1_7_R4.CrashReport;
import net.minecraft.server.v1_7_R4.CrashReportSystemDetails;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.ItemStack;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;
import net.minecraft.server.v1_7_R4.ReportedException;
import net.minecraft.util.org.apache.commons.lang3.ObjectUtils;

public class DataWatcherLegacy {

    private final Entity a;
    private boolean b = true;
    private static final HashMap c = new HashMap();
    private final Map d = new HashMap();
    private boolean e;
    private ReadWriteLock f = new ReentrantReadWriteLock();

    public DataWatcherLegacy(Entity entity) {
        this.a = entity;
    }

    public void a(int i, Object object) {
        Integer integer = (Integer) c.get(object.getClass());

        if (integer == null) {
            throw new IllegalArgumentException("Unknown data type: " + object.getClass());
        } else if (i > 31) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is " + 31 + ")");
        } else if (this.d.containsKey(Integer.valueOf(i))) {
            throw new IllegalArgumentException("Duplicate id value for " + i + "!");
        } else {
            WatchableObjectLegacy WatchableObjectLegacy = new WatchableObjectLegacy(integer.intValue(), i, object);

            this.f.writeLock().lock();
            this.d.put(Integer.valueOf(i), WatchableObjectLegacy);
            this.f.writeLock().unlock();
            this.b = false;
        }
    }

    public void add(int i, int j) {
        WatchableObjectLegacy WatchableObjectLegacy = new WatchableObjectLegacy(j, i, null);

        this.f.writeLock().lock();
        this.d.put(Integer.valueOf(i), WatchableObjectLegacy);
        this.f.writeLock().unlock();
        this.b = false;
    }

    public byte getByte(int i) {
        return ((Byte) this.i(i).b()).byteValue();
    }

    public short getShort(int i) {
        return ((Short) this.i(i).b()).shortValue();
    }

    public int getInt(int i) {
        return ((Integer) this.i(i).b()).intValue();
    }

    public float getFloat(int i) {
        return ((Float) this.i(i).b()).floatValue();
    }

    public String getString(int i) {
        return (String) this.i(i).b();
    }

    public ItemStack getItemStack(int i) {
        return (ItemStack) this.i(i).b();
    }

    private WatchableObjectLegacy i(int i) {
        this.f.readLock().lock();

        WatchableObjectLegacy WatchableObjectLegacy;

        try {
            WatchableObjectLegacy = (WatchableObjectLegacy) this.d.get(Integer.valueOf(i));
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Getting synched entity data");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Synched entity data");

            crashreportsystemdetails.a("Data ID", Integer.valueOf(i));
            throw new ReportedException(crashreport);
        }

        this.f.readLock().unlock();
        return WatchableObjectLegacy;
    }

    public void watch(int i, Object object) {
        WatchableObjectLegacy WatchableObjectLegacy = this.i(i);

        if (ObjectUtils.notEqual(object, WatchableObjectLegacy.b())) {
            WatchableObjectLegacy.a(object);
            this.a.i(i);
            WatchableObjectLegacy.a(true);
            this.e = true;
        }
    }

    public void update(int i) {
        WatchableObjectLegacy.a(this.i(i), true);
        this.e = true;
    }

    public boolean a() {
        return this.e;
    }

    public static void a(List list, PacketDataSerializerLegacy packetdataserializer) {
        if (list != null) {
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                WatchableObjectLegacy WatchableObjectLegacy = (WatchableObjectLegacy) iterator.next();

                a(packetdataserializer, WatchableObjectLegacy);
            }
        }

        packetdataserializer.writeByte(127);
    }

    public List b() {
        ArrayList arraylist = null;

        if (this.e) {
            this.f.readLock().lock();
            Iterator iterator = this.d.values().iterator();

            while (iterator.hasNext()) {
                WatchableObjectLegacy WatchableObjectLegacy = (WatchableObjectLegacy) iterator.next();

                if (WatchableObjectLegacy.d()) {
                    WatchableObjectLegacy.a(false);
                    if (arraylist == null) {
                        arraylist = new ArrayList();
                    }

                    arraylist.add(WatchableObjectLegacy);
                }
            }

            this.f.readLock().unlock();
        }

        this.e = false;
        return arraylist;
    }

    public void a(PacketDataSerializerLegacy packetdataserializer) {
        this.f.readLock().lock();
        Iterator iterator = this.d.values().iterator();

        while (iterator.hasNext()) {
            WatchableObjectLegacy WatchableObjectLegacy = (WatchableObjectLegacy) iterator.next();

            a(packetdataserializer, WatchableObjectLegacy);
        }

        this.f.readLock().unlock();
        packetdataserializer.writeByte(127);
    }

    public List c() {
        ArrayList arraylist = null;

        this.f.readLock().lock();

        WatchableObjectLegacy WatchableObjectLegacy;

        for (Iterator iterator = this.d.values().iterator(); iterator.hasNext(); arraylist.add(WatchableObjectLegacy)) {
            WatchableObjectLegacy = (WatchableObjectLegacy) iterator.next();
            if (arraylist == null) {
                arraylist = new ArrayList();
            }
        }

        this.f.readLock().unlock();
        return arraylist;
    }

    private static void a(PacketDataSerializerLegacy packetdataserializer, WatchableObjectLegacy WatchableObjectLegacy) {
        int i = (WatchableObjectLegacy.c() << 5 | WatchableObjectLegacy.a() & 31) & 255;

        packetdataserializer.writeByte(i);
        switch (WatchableObjectLegacy.c()) {
        case 0:
            packetdataserializer.writeByte(((Byte) WatchableObjectLegacy.b()).byteValue());
            break;

        case 1:
            packetdataserializer.writeShort(((Short) WatchableObjectLegacy.b()).shortValue());
            break;

        case 2:
            packetdataserializer.writeInt(((Integer) WatchableObjectLegacy.b()).intValue());
            break;

        case 3:
            packetdataserializer.writeFloat(((Float) WatchableObjectLegacy.b()).floatValue());
            break;

        case 4:
            try {
                packetdataserializer.a((String) WatchableObjectLegacy.b());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            break;

        case 5:
            ItemStack itemstack = (ItemStack) WatchableObjectLegacy.b();

            packetdataserializer.a(itemstack);
            break;

        case 6:
            ChunkCoordinates chunkcoordinates = (ChunkCoordinates) WatchableObjectLegacy.b();

            packetdataserializer.writeInt(chunkcoordinates.x);
            packetdataserializer.writeInt(chunkcoordinates.y);
            packetdataserializer.writeInt(chunkcoordinates.z);
        }
    }

    public static List b(PacketDataSerializerLegacy packetdataserializer) {
        ArrayList arraylist = null;

        for (byte b0 = packetdataserializer.readByte(); b0 != 127; b0 = packetdataserializer.readByte()) {
            if (arraylist == null) {
                arraylist = new ArrayList();
            }

            int i = (b0 & 224) >> 5;
            int j = b0 & 31;
            WatchableObjectLegacy WatchableObjectLegacy = null;

            switch (i) {
            case 0:
                WatchableObjectLegacy = new WatchableObjectLegacy(i, j, Byte.valueOf(packetdataserializer.readByte()));
                break;

            case 1:
                WatchableObjectLegacy = new WatchableObjectLegacy(i, j, Short.valueOf(packetdataserializer.readShort()));
                break;

            case 2:
                WatchableObjectLegacy = new WatchableObjectLegacy(i, j, Integer.valueOf(packetdataserializer.readInt()));
                break;

            case 3:
                WatchableObjectLegacy = new WatchableObjectLegacy(i, j, Float.valueOf(packetdataserializer.readFloat()));
                break;

            case 4:
                try {
                    WatchableObjectLegacy = new WatchableObjectLegacy(i, j, packetdataserializer.c(32767));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;

            case 5:
                WatchableObjectLegacy = new WatchableObjectLegacy(i, j, packetdataserializer.c());
                break;

            case 6:
                int k = packetdataserializer.readInt();
                int l = packetdataserializer.readInt();
                int i1 = packetdataserializer.readInt();

                WatchableObjectLegacy = new WatchableObjectLegacy(i, j, new ChunkCoordinates(k, l, i1));
            }

            arraylist.add(WatchableObjectLegacy);
        }

        return arraylist;
    }

    public boolean d() {
        return this.b;
    }

    public void e() {
        this.e = false;
    }

    static {
        c.put(Byte.class, Integer.valueOf(0));
        c.put(Short.class, Integer.valueOf(1));
        c.put(Integer.class, Integer.valueOf(2));
        c.put(Float.class, Integer.valueOf(3));
        c.put(String.class, Integer.valueOf(4));
        c.put(ItemStack.class, Integer.valueOf(5));
        c.put(ChunkCoordinates.class, Integer.valueOf(6));
    }
}