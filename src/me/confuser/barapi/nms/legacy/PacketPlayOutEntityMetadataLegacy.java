package me.confuser.barapi.nms.legacy;

import java.util.List;

import net.minecraft.server.v1_7_R4.DataWatcher;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;
import net.minecraft.server.v1_7_R4.PacketListener;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_7_R4.PacketPlayOutListener;

public class PacketPlayOutEntityMetadataLegacy extends PacketPlayOutEntityMetadata {

    private int a;
    private List b;

    public PacketPlayOutEntityMetadataLegacy(int i, DataWatcherLegacy datawatcher, boolean flag) {
        this.a = i;
        if (flag) {
            this.b = datawatcher.c();
        } else {
            this.b = datawatcher.b();
        }
    }

    public void a(PacketDataSerializerLegacy packetdataserializer) {
        this.a = packetdataserializer.readInt();
        this.b = DataWatcherLegacy.b(packetdataserializer);
    }

    public void b(PacketDataSerializerLegacy packetdataserializer) {
        packetdataserializer.writeInt(this.a);
        DataWatcherLegacy.a(this.b, packetdataserializer);
    }

    public void a(PacketPlayOutListener packetplayoutlistener) {
        packetplayoutlistener.a(this);
    }

    public void handle(PacketListener packetlistener) {
        this.a((PacketPlayOutListener) packetlistener);
    }
}
