import asyncio
import struct
import socket

data = [(26.1025384,44.4267674),
        (26.0408043,44.4332409),
        (26.0094856,44.4343161),
        (26.0056447,44.4342855)]

@asyncio.coroutine
def write_location_data(transport, data, addr, delay):
    for item in data:
        print(item)
        packed_data = struct.pack('!dd',*item)
        # string_data = ""
        # for byte in packed_data:
        #     string_data += "%02X " % byte
        # print(string_data)
        transport.sendto(packed_data, addr)
        yield from asyncio.sleep(delay)

class MyDataGramProtocol(asyncio.DatagramProtocol):

    def connection_made(self, transport):
        super().connection_made(transport)
        self._transport = transport
        sock = transport.get_extra_info("socket")
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        print('connection made')

    def connection_lost(self, exc):
        super().connection_lost(exc)
        print('connection lost')

    def datagram_received(self, data, addr):
        super().datagram_received(data, addr)
        print("{}: {}".format(addr, struct.unpack('=dd',data)))


if __name__=="__main__":

    loop = asyncio.get_event_loop()
    udp_server = loop.create_datagram_endpoint(MyDataGramProtocol, ('0.0.0.0', 9999))
    transport, protocol = loop.run_until_complete(udp_server)
    try:
        loop.create_task(write_location_data(transport, data, ('10.1.12.255', 8888), 5))
        loop.run_forever()
    except KeyboardInterrupt:
        loop.stop()
        transport.close()
        loop.close()