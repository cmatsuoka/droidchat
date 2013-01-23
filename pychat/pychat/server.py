#-*- coding:utf-8 -*-

import socket
import select
import threading
import time
from common import *


class ChatServer:
	_client = {} # _client[name] = (socket, (ip, port))
	_messages = []
	_sock = None
	_listenConnThread = None
	_listenMsgThread = None
	_end = False

	def __init__(self):
		# create socket
		self._sock = socket.socket()
		self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
		self._sock.bind(('', SERVER_PORT))
		self._sock.listen(LISTEN_QUEUE_SIZE)

	def start(self):
		self._end = False

		self._messages = []
		self._lastID = MSG_MAX_KEEP - 1
		for i in range(MSG_MAX_KEEP):
			self._messages.append(None)

		self._listenConnThread = threading.Thread(target = self._listenConnections)
		self._listenMsgThread = threading.Thread(target = self._listenMsg)

		self._listenConnThread.start()
		self._listenMsgThread.start()

	def stop(self):
		self._end = True

		try:
			self._listenConnThread.join()
		except:
			pass

		try:
			self._listenMsgThread.join()
		except:
			pass

	# listen to new clients connections
	def _listenConnections(self):
		while not self._end:
			try:
				self._sock.settimeout(LISTEN_TIMEOUT)
				sock, addr = self._sock.accept()
				print "New connection !"
				threading.Thread(target = self._newClient, args = (sock, addr)).start()
			except socket.timeout:
				pass

	def _newClient(self, sock, addr):
		name = self._receiveMsg(sock = sock)
		self._client[name] = (sock, addr)

		print "New client '%s': %s: " % (name, str(self._client[name]))

	def _removeClient(self, name):
		self._client.pop(name)
		print "Client %s left !" % (name)

	# listen for messages for MSG_INTERVAL seconds,
	# then send all messages received to all clients, 
	# including the list of connected clients
	def _listenMsg(self):
		past = 0
		while not self._end:
			socks = []
			for i in self._client.keys():
				socks.append(self._client[i][0])

			# if no clients connected, do nothing
			if socks == []:
				time.sleep(MSG_INTERVAL)
				continue

			now = time.time()

			try:
				rlist, wlist, xlist = select.select(socks, [], [], MSG_INTERVAL - past)
			except:
				continue

			# timeout
			if rlist == []:
				past = 0
				try:
					msg = self._packList()
					for i in self._client.keys():
						self._sendMsg(self._client[i][0], msg)
				except:
					pass
				continue

			aux = time.time()
			past += aux - now
			now = aux

			msg = self._receiveMsg(sock = rlist[0])
			cli = self._findClientBySocket(rlist[0])

			if msg == "":
				self._removeClient(cli)
			else:
				self._newMessage(cli, msg)

	def _packList(self):
		clientsMsg = MSG_SEP.join(self._client.keys())
		
		messages = []
		i = self._lastID
		while True:
			i = (i + 1) % MSG_MAX_KEEP
			if self._messages[i] == None:
				if i == self._lastID:
					break
				continue

			name, msg = self._messages[i]
			messages.append("@".join([str(i), name, msg]))
			if i == self._lastID:
				break

		messagesMsg = MSG_SEP.join(messages)

		return "#".join([clientsMsg, messagesMsg])


	def _newMessage(self, name, msg):
		self._lastID = (self._lastID + 1) % MSG_MAX_KEEP
		self._messages[self._lastID] = (name, msg)

	def _findClientBySocket(self, sock):
		for i in self._client.keys():
			if self._client[i][0] == sock:
				return i
		return None

	# receive msg
	def _receiveMsg(self, timeout = None, sock = None):
		if sock == None:
			sock = self._sock
		sock.settimeout(timeout)
		try:
			return sock.recv(MSG_MAX_BYTES)
		except:
			return None

	# send msg
	def _sendMsg(self, sock, msg):
		try:
			sock.sendall(msg)
			return True
		except:
			return False

