protobuf_dest=protobufs
protobuf_source=../protocol

run:
	REDIS_HOST=localhost bl -r

python_proto: 
	rm -rf generated $(protobuf_dest)
	mkdir -p generated
	cp -r $(protobuf_source) $(protobuf_dest)
	python -m grpc_tools.protoc -I $(protobuf_dest) --python_out=./generated/ --grpc_python_out=./generated/ $(protobuf_dest)/**/*.proto
	find generated -name "*.py" | xargs -I {} sed -i -E 's/from (api|common)/from generated.\1/g' {}
