from skimage import io,transform
import tensorflow as tf
import numpy as np

path1 = "D:/Download/CoinsDatasets/test_photos/1euro.jpg"
path2 = "D:/Download/CoinsDatasets/test_photos/2cent.jpg"
path3 = "D:/Download/CoinsDatasets/test_photos/2euro.jpg"
path4 = "D:/Download/CoinsDatasets/test_photos/10cent.jpg"
path5 = "D:/Download/CoinsDatasets/test_photos/50cent.jpg"

flower_dict = {0:'1 euro',1:'2 cent',2:'2 euro',3:'10 cent',4:'50 cent'}

w=100
h=100
c=3

def read_one_image(path):
    img = io.imread(path)
    img = transform.resize(img,(w,h))
    return np.asarray(img)

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

    saver = tf.train.import_meta_graph('D:/Download/CoinsDatasets/model/coins/model.ckpt.meta')
    saver.restore(sess,tf.train.latest_checkpoint('D:/Download/CoinsDatasets/model/coins/'))

    graph = tf.get_default_graph()
    x = graph.get_tensor_by_name("x:0")
    feed_dict = {x:data}

    logits = graph.get_tensor_by_name("logits_eval:0")

    classification_result = sess.run(logits,feed_dict)

    #打印出预测矩阵
    print(classification_result)
    #打印出预测矩阵每一行最大值的索引
    print(tf.argmax(classification_result,1).eval())
    #根据索引通过字典对应花的分类
    output = []
    output = tf.argmax(classification_result,1).eval()
    for i in range(len(output)):
        print("第",i+1,"个硬币预测:"+flower_dict[output[i]])