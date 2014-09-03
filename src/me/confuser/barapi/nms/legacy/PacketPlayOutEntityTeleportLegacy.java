package me.confuser.barapi.nms.legacy;

import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.MathHelper;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;
import net.minecraft.server.v1_7_R4.PacketListener;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_7_R4.PacketPlayOutListener;

public class PacketPlayOutEntityTeleportLegacy extends PacketPlayOutEntityTeleport {

    private int a;
    private int b;
    private int c;
    private int d;
    private byte e;
    private byte f;

    public PacketPlayOutEntityTeleportLegacy() {}

    public PacketPlayOutEntityTeleportLegacy(Entity entity) {
        this.a = entity.getId();
        this.b = MathHelper.floor(entity.locX * 32.0D);
        this.c = MathHelper.floor(entity.locY * 32.0D);
        this.d = MathHelper.floor(entity.locZ * 32.0D);
        this.e = (byte) ((int) (entity.yaw * 256.0F / 360.0F));
        this.f = (byte) ((int) (entity.pitch * 256.0F / 360.0F));
    }

    public PacketPlayOutEntityTeleportLegacy(int i, int j, int k, int l, byte b0, byte b1) {
        this.a = i;
        this.b = j;
        this.c = k;
        this.d = l;
        this.e = b0;
        this.f = b1;
    }

    public void a(PacketDataSerializer packetdataserializer) {
        this.a = packetdataserializer.readInt();
        this.b = packetdataserializer.readInt();
        this.c = packetdataserializer.readInt();
        this.d = packetdataserializer.readInt();
        this.e = packetdataserializer.readByte();
        this.f = packetdataserializer.readByte();
    }

    public void b(PacketDataSerializer packetdataserializer) {
        // Spigot start - protocol
        if ( packetdataserializer.version < 16 ){
            packetdataserializer.writeInt( this.a );
        } else{
            packetdataserializer.b( a );
        }
        // Spigot end

        packetdataserializer.writeInt(this.a);
        packetdataserializer.writeInt(this.b);
        packetdataserializer.writeInt(this.c);
        packetdataserializer.writeInt(this.d);
        packetdataserializer.writeByte(this.e);
        packetdataserializer.writeByte(this.f);

        // Spigot start - protocol patch
        /*if ( packetdataserializer.version >= 22 ) {
            packetdataserializer.writeBoolean( onGround );
        }*/
        // Spigot end
    }

    public void a(PacketPlayOutListener packetplayoutlistener) {
        packetplayoutlistener.a(this);
    }

    public void handle(PacketListener packetlistener) {
        this.a((PacketPlayOutListener) packetlistener);
    }
}
