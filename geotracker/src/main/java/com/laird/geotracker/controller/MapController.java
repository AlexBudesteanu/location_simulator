package com.laird.geotracker.controller;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ResourceBundle;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.udp.server.UdpServer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import rx.Observable;
import rx.functions.Func1;

class RxUDPSever {

	private int port;
	private GoogleMap map;

	public RxUDPSever(int port, GoogleMap map) {
		this.port = port;
		this.map = map;
	}

	public UdpServer<DatagramPacket, DatagramPacket> createServer() {
		UdpServer<DatagramPacket, DatagramPacket> server = RxNetty.createUdpServer(this.port,
				new ConnectionHandler<DatagramPacket, DatagramPacket>() {

					@Override
					public Observable<Void> handle(ObservableConnection<DatagramPacket, DatagramPacket> newConnection) {

						return newConnection.getInput().flatMap(new Func1<DatagramPacket, Observable<Void>>() {

							@Override
							public Observable<Void> call(DatagramPacket received) {
								//InetSocketAddress sender = received.sender();
//								System.out.println("Received datagram. Sender: " + sender + ", data: "
//										+ received.content());
								// TODO: put marker on map.
								ByteBuf buffer = received.content();
								byte[] bytes = new byte[buffer.readableBytes()];
								int readerIndex = buffer.readerIndex();
								buffer.getBytes(readerIndex, bytes);
								byte[] latBytes = Arrays.copyOfRange(bytes, 0, 9);
								//byte[] lonBytes = Arrays.copyOfRange(bytes, 9, 15);
								double lat = ByteBuffer.wrap(latBytes).getDouble();
								//double lon = ByteBuffer.wrap(lonBytes).getDouble();
								System.out.println(String.format("%f", lat));
								
								return Observable.just(null);
							}
						});
					}
				});

		return server;
	}
}

public class MapController implements Initializable, MapComponentInitializedListener {

	@FXML
	protected GoogleMapView mapView;

	@FXML
	protected Button startButton;

	@FXML
	protected Button stopButton;
	
	UdpServer<DatagramPacket, DatagramPacket> server;
	GoogleMap map;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		mapView.addMapInializedListener(this);
		startButton.setOnMouseClicked(event -> {
			try {
				if(server == null)
					server = new RxUDPSever(8888, map).createServer();
				server.start();
			} catch (IllegalStateException e) {
				System.out.println("Server already started.");
			}
		});
		stopButton.setOnMouseClicked(event -> {
			try {
				server.shutdown();
				if(server != null)
					server = null;
			}  catch (IllegalStateException e) {
				System.out.println("Server already closed.");
			}  catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void mapInitialized() {
		MapOptions options = new MapOptions();

		options.center(new LatLong(44.4267674,26.1025384)).zoomControl(true).zoom(10).mapType(MapTypeIdEnum.ROADMAP);
		map = mapView.createMap(options);
	}

}
