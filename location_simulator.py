import asyncio
import struct

data = [(44.4267674,26.1025384),
        (44.4332409,26.0408043),
        (44.4343161,26.0094856),
        (44.4342855,26.0056447)]

@asyncio.coroutine
def write_location_data(transport, data, addr, delay):
    for item in data:
        packed_data = struct.pack('=dd',*item)
        transport.sendto(packed_data, addr)
        yield from asyncio.sleep(delay)

class MyDataGramProtocol(asyncio.DatagramProtocol):

    def connection_made(self, transport):
        super().connection_made(transport)
        self._transport = transport
        print('connection made')

    def connection_lost(self, exc):
        super().connection_lost(exc)
        print('connection lost')

    def datagram_received(self, data, addr):
        super().datagram_received(data, addr)
        print("{}: {}".format(addr, struct.unpack('=dd',data)))


if __name__=="__main__":

    loop = asyncio.get_event_loop()
    udp_server = loop.create_datagram_endpoint(MyDataGramProtocol, ('127.0.0.1', 9999))
    transport, protocol = loop.run_until_complete(udp_server)
    try:
        loop.create_task(write_location_data(transport, data, ('10.1.12.92', 2527), 5))
        loop.run_forever()
    except KeyboardInterrupt:
        loop.stop()
        transport.close()
        loop.close()