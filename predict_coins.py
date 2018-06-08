from skimage import io,transform
import tensorflow as tf
import numpy as np

path1 = "/home/yaoling/Desktop/githubProject/CoinClassifier/Coindatasets/testPhoto/1euro.jpg"
path2 = "/home/yaoling/Desktop/githubProject/CoinClassifier/Coindatasets/testPhoto/2cent.jpg"
path3 = "/home/yaoling/Desktop/githubProject/CoinClassifier/Coindatasets/testPhoto/2euro.jpg"
path4 = "/home/yaoling/Desktop/githubProject/CoinClassifier/Coindatasets/testPhoto/10cent.jpg"
path5 = "/home/yaoling/Desktop/githubProject/CoinClassifier/Coindatasets/testPhoto/50cent.jpg"
flower_dict = {0:'1 euro',1:'2 euro',2:'10 cent',3:'50 cent',4:'2 cent'}

w=224
h=224
c=3

def read_one_image(path):
    img = io.imread(path)
    img = transform.resize(img,(w,h))
    return np.asarray(img)


with tf.Graph().as_default():
    output_graph_def = tf.GraphDef()
    output_graph_path = '/home/yaoling/Desktop/githubProject/CoinClassifier/Coindatasets/model/model.pb'
    #sess.graph.add_to_collection("input", mnist.test.images)

    with open(output_graph_path, "rb") as f:
        output_graph_def.ParseFromString(f.read())
        _ = tf.import_graph_def(output_graph_def, name="")

    with tf.Session() as sess:
        data = []
        data1 = read_one_image(path1)
        data2 = read_one_image(path2)
        data3 = read_one_image(path3)
        data4 = read_one_image(path4)
        data5 = read_one_image(path5)
        data.append(data1)
        data.append(data2)
        data.append(data3)
        data.append(data4)
        data.append(data5)

        # saver = tf.train.import_meta_graph('/home/yaoling/Desktop/githubProject/CoinClassifier/CoinsDatasets/model/model.ckpt.meta')
        # saver.restore(sess,tf.train.latest_checkpoint('/home/yaoling/Desktop/githubProject/CoinClassifier/CoinsDatasets/model/'))

        graph = tf.get_default_graph()
        x = graph.get_tensor_by_name("x:0")
        feed_dict = {x: data}

        logits = graph.get_tensor_by_name("logits_eval:0")

        classification_result = sess.run(logits, feed_dict)

        # 打印出预测矩阵
        print(classification_result)
        # 打印出预测矩阵每一行最大值的索引
        print(tf.argmax(classification_result, 1).eval())
        # 根据索引通过字典对应花的分类
        output = []
        output = tf.argmax(classification_result, 1).eval()
        for i in range(len(output)):
            print("The", i + 1, "coin prediction"
                                ":" + flower_dict[output[i]])

