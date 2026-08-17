# Руководство по интеграции с Сервером (Paper / Purpur Plugin)

Этот документ описывает, как серверный плагин на Paper/Purpur (1.20.x - 1.21.x) общается с клиентским модом **CustomBlockLayer**.

---

## 📡 Сетевой протокол (Plugin Channels)

Каналы сообщений (Plugin Messaging Channels):
* `customblocklayer:handshake` — клиент при входе шлет версию мода (`"0.1"`).
* `customblocklayer:set_block` — сервер шлет клиенту установку 1 блока (`BlockPos` + `String id`).
* `customblocklayer:clear_block` — сервер шлет клиенту удаление 1 блока (`BlockPos`).
* `customblocklayer:chunk_blocks` — сервер шлет клиенту все кастомные блоки чанка при входе в чанк.

---

## 💾 Хранение данных на Сервере (Chunk PDC)

Используйте `Chunk.getPersistentDataContainer()`:
* **Ключ:** `NamespacedKey(plugin, "cbl_blocks")`
* **Формат:** `PersistentDataType.STRING` (JSON-строка `Map<Short, String>`) или примитивный байтовый массив.

---

## 💻 Готовый пример кода для Paper Плагина

### 1. Регистрация каналов в `onEnable()`:
```java
@Override
public void onEnable() {
    getServer().getMessenger().registerOutgoingPluginChannel(this, "customblocklayer:set_block");
    getServer().getMessenger().registerOutgoingPluginChannel(this, "customblocklayer:clear_block");
    getServer().getMessenger().registerOutgoingPluginChannel(this, "customblocklayer:chunk_blocks");
    
    getServer().getMessenger().registerIncomingPluginChannel(this, "customblocklayer:handshake", (channel, player, message) -> {
        // Игрок имеет мод CBL! Помечаем его в памяти и отсылаем блоки вокруг него
        onPlayerCBLHandshake(player);
    });
}
```

### 2. Отправка пакета установки блока (`BlockPlaceEvent`):
```java
@EventHandler
public void onBlockPlace(BlockPlaceEvent event) {
    ItemStack item = event.getItemInHand();
    // Читаем NBT / PDC предмета
    String cblId = getCBLIdFromItem(item);
    if (cblId != null) {
        Location loc = event.getBlock().getLocation();
        saveBlockToChunkPDC(loc.getChunk(), loc, cblId);
        
        // Отправляем всем игрокам, видящим этот чанк
        sendSetBlockPacket(loc, cblId);
    }
}

public void sendSetBlockPacket(Location loc, String cblId) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    // Сериализация BlockPos (long в формате Minecraft)
    long packedPos = ((long)(loc.getBlockX() & 0x3FFFFFF) << 38) |
                     ((long)(loc.getBlockZ() & 0x3FFFFFF) << 12) |
                     ((long)(loc.getBlockY() & 0xFFF));
    out.writeLong(packedPos);
    out.writeUTF(cblId);

    byte[] data = out.toByteArray();
    for (Player p : loc.getWorld().getPlayers()) {
        if (isCBLUser(p) && p.getLocation().distanceSquared(loc) < 128 * 128) {
            p.sendPluginMessage(this, "customblocklayer:set_block", data);
        }
    }
}
```

### 3. Отправка пакета удаления блока (`BlockBreakEvent`):
```java
@EventHandler
public void onBlockBreak(BlockBreakEvent event) {
    Location loc = event.getBlock().getLocation();
    if (isCustomBlock(loc)) {
        removeBlockFromChunkPDC(loc.getChunk(), loc);
        
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        long packedPos = ((long)(loc.getBlockX() & 0x3FFFFFF) << 38) |
                         ((long)(loc.getBlockZ() & 0x3FFFFFF) << 12) |
                         ((long)(loc.getBlockY() & 0xFFF));
        out.writeLong(packedPos);
        
        byte[] data = out.toByteArray();
        for (Player p : loc.getWorld().getPlayers()) {
            if (isCBLUser(p)) {
                p.sendPluginMessage(this, "customblocklayer:clear_block", data);
            }
        }
    }
}
```
